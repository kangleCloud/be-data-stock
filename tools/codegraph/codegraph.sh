#!/usr/bin/env bash

set -euo pipefail

readonly CODEGRAPH_PACKAGE="@colbymchenry/codegraph"
readonly CODEGRAPH_VERSION="1.5.0"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

export CODEGRAPH_TELEMETRY=0
export CODEGRAPH_NO_UPDATE_CHECK=1
export CODEGRAPH_MCP_TOOLS=explore

log() {
    printf '[codegraph] %s\n' "$*"
}

warn() {
    printf '[codegraph] WARNING: %s\n' "$*" >&2
}

die() {
    printf '[codegraph] ERROR: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: ./tools/codegraph/codegraph.sh <command> [arguments]

Commands:
  setup                 Install CodeGraph 1.5.0, rebuild personal MCP entries,
                        and initialize or sync the repository index.
  init                  Initialize the root index, or sync it when it exists.
  status                Check CLI version, telemetry, index, ignore rule, and MCP entries.
  sync                  Incrementally synchronize the root index.
  reindex               Force a full rebuild of the root index.
  explore <query>       Explore an exact symbol or call-path query.
  teardown --yes        Remove the root index, personal MCP entries, and global package.
  help                  Show this help.

Environment:
  CODEGRAPH_OUTPUT_MAX_LINES  Maximum explore output lines (default: 400).

Only setup and teardown --yes modify the developer's personal environment.
EOF
}

ensure_repository() {
    [ -f "${REPO_ROOT}/pom.xml" ] || die "Repository root marker pom.xml is missing: ${REPO_ROOT}"
    [ -f "${REPO_ROOT}/AICoding/rules/be-vita-ai-codegen-rules.md" ] \
        || die "Repository rule file is missing under ${REPO_ROOT}"
    cd "${REPO_ROOT}"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "Required command is not available: $1"
}

ensure_codegraph_version() {
    local version_output

    require_command codegraph
    version_output="$(codegraph version 2>&1)" || die "Unable to read the CodeGraph version"
    printf '%s\n' "${version_output}" | grep -Eq '(^|[^0-9])1\.5\.0([^0-9]|$)' \
        || die "CodeGraph ${CODEGRAPH_VERSION} is required, found: ${version_output}"
}

require_index() {
    [ -d "${REPO_ROOT}/.codegraph" ] \
        || die "CodeGraph index is missing. Run ./tools/codegraph/codegraph.sh init first."
}

initialize_index() {
    ensure_codegraph_version
    if [ -d "${REPO_ROOT}/.codegraph" ]; then
        log "Index exists; synchronizing ${REPO_ROOT}"
        codegraph sync
    else
        log "Initializing index at ${REPO_ROOT}"
        codegraph init
    fi
}

mcp_entry_exists() {
    local cli="$1"

    "${cli}" mcp list 2>/dev/null | awk '
        $1 == "codegraph" { found = 1 }
        END { exit found ? 0 : 1 }
    '
}

rebuild_codex_mcp() {
    local codegraph_bin="$1"

    if ! command -v codex >/dev/null 2>&1; then
        warn "Codex CLI is not installed; skipping Codex MCP registration"
        return
    fi
    if mcp_entry_exists codex; then
        codex mcp remove codegraph >/dev/null
    fi
    codex mcp add codegraph \
        --env CODEGRAPH_TELEMETRY=0 \
        --env CODEGRAPH_NO_UPDATE_CHECK=1 \
        --env CODEGRAPH_MCP_TOOLS=explore \
        -- "${codegraph_bin}" serve --mcp
}

rebuild_claude_mcp() {
    local codegraph_bin="$1"

    if ! command -v claude >/dev/null 2>&1; then
        warn "Claude CLI is not installed; skipping Claude MCP registration"
        return
    fi
    if mcp_entry_exists claude; then
        claude mcp remove codegraph >/dev/null
    fi
    claude mcp add codegraph --scope user \
        -e CODEGRAPH_TELEMETRY=0 \
        -e CODEGRAPH_NO_UPDATE_CHECK=1 \
        -e CODEGRAPH_MCP_TOOLS=explore \
        -- "${codegraph_bin}" serve --mcp
}

setup() {
    local codegraph_bin

    require_command npm
    log "Installing ${CODEGRAPH_PACKAGE}@${CODEGRAPH_VERSION}"
    npm install -g "${CODEGRAPH_PACKAGE}@${CODEGRAPH_VERSION}"
    hash -r
    ensure_codegraph_version
    codegraph telemetry off
    codegraph_bin="$(command -v codegraph)"
    rebuild_codex_mcp "${codegraph_bin}"
    rebuild_claude_mcp "${codegraph_bin}"
    initialize_index
    log "Setup complete. Restart installed agents before using MCP tools."
}

status() {
    local failed=0

    ensure_codegraph_version
    require_index
    codegraph version
    codegraph telemetry status
    codegraph status
    require_command git
    if git check-ignore -q .codegraph/codegraph.db; then
        log ".codegraph/ is ignored by Git"
    else
        warn ".codegraph/ is not ignored by Git"
        failed=1
    fi

    if command -v codex >/dev/null 2>&1 && ! codex mcp get codegraph --json; then
        warn "Codex MCP entry is missing or invalid"
        failed=1
    fi
    if command -v claude >/dev/null 2>&1 && ! claude mcp get codegraph; then
        warn "Claude MCP entry is missing or invalid"
        failed=1
    fi

    [ "${failed}" -eq 0 ] || return 1
}

sync_index() {
    ensure_codegraph_version
    require_index
    codegraph sync
}

reindex() {
    ensure_codegraph_version
    require_index
    codegraph index --force
}

explore() {
    local max_lines="${CODEGRAPH_OUTPUT_MAX_LINES:-400}"
    local query

    [ "$#" -gt 0 ] || die "Explore query must not be empty"
    case "${max_lines}" in
        ''|*[!0-9]*) die "CODEGRAPH_OUTPUT_MAX_LINES must be a positive integer" ;;
    esac
    [ "${max_lines}" -gt 0 ] || die "CODEGRAPH_OUTPUT_MAX_LINES must be greater than zero"

    ensure_codegraph_version
    require_index
    query="$*"
    codegraph explore "${query}" 2>&1 | awk -v limit="${max_lines}" '
        NR <= limit { print; next }
        NR == limit + 1 { print "[codegraph] Output truncated at " limit " lines." }
    '
}

remove_mcp_entries() {
    if command -v codex >/dev/null 2>&1 && mcp_entry_exists codex; then
        codex mcp remove codegraph
    fi
    if command -v claude >/dev/null 2>&1 && mcp_entry_exists claude; then
        claude mcp remove codegraph
    fi
}

teardown() {
    [ "${1:-}" = "--yes" ] || die "teardown requires explicit confirmation: teardown --yes"

    remove_mcp_entries
    if command -v codegraph >/dev/null 2>&1 && [ -d "${REPO_ROOT}/.codegraph" ]; then
        codegraph uninit --force
    fi
    # 仅在显式确认后清理由官方命令未移除的仓库根索引。
    if [ -d "${REPO_ROOT}/.codegraph" ]; then
        rm -rf -- "${REPO_ROOT}/.codegraph"
    fi
    if command -v npm >/dev/null 2>&1; then
        npm uninstall -g "${CODEGRAPH_PACKAGE}"
    else
        warn "npm is not installed; global package removal was skipped"
    fi
    log "Teardown complete; repository scripts were retained."
}

main() {
    local command="${1:-help}"

    ensure_repository
    if [ "$#" -gt 0 ]; then
        shift
    fi

    case "${command}" in
        setup) [ "$#" -eq 0 ] || die "setup does not accept arguments"; setup ;;
        init) [ "$#" -eq 0 ] || die "init does not accept arguments"; initialize_index ;;
        status) [ "$#" -eq 0 ] || die "status does not accept arguments"; status ;;
        sync) [ "$#" -eq 0 ] || die "sync does not accept arguments"; sync_index ;;
        reindex) [ "$#" -eq 0 ] || die "reindex does not accept arguments"; reindex ;;
        explore) explore "$@" ;;
        teardown) [ "$#" -le 1 ] || die "teardown only accepts --yes"; teardown "${1:-}" ;;
        help|-h|--help) [ "$#" -eq 0 ] || die "help does not accept arguments"; usage ;;
        *) usage >&2; die "Unknown command: ${command}" ;;
    esac
}

main "$@"
