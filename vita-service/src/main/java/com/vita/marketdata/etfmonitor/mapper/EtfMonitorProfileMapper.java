package com.vita.marketdata.etfmonitor.mapper;

import com.vita.marketdata.etfmonitor.entity.EtfMonitorProfile;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EtfMonitorProfileMapper extends BaseMapperX<EtfMonitorProfile> {
    /** 同花顺成功资料按本次值完整替换，显式 NULL 防止 MP 忽略旧字段清理。 */
    @Update("""
            UPDATE etf_monitor_profile
            SET full_name=#{fullName}, fund_type=#{fundType}, investment_type=#{investmentType},
                fund_manager=#{fundManager}, established_date=#{establishedDate},
                performance_benchmark=#{performanceBenchmark}, source=#{source},
                manager=#{manager}, custodian=#{custodian}, exchange=#{exchange}, etf_type=#{etfType},
                tracking_index_code=#{trackingIndexCode}, tracking_index_name=#{trackingIndexName},
                listing_status=NULL, listing_date=NULL, share_count=NULL, share_date=NULL,
                profile_updated_at=#{profileUpdatedAt}, update_time=NOW()
            WHERE id=#{id} AND symbol=#{symbol} AND is_deleted=0
            """)
    int updateThsProfile(EtfMonitorProfile profile);
}
