package com.vita.repeat.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RepeatSubmitUtilsTest {

    @Test
    void digestArgsShouldIgnoreMapAndCollectionOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("b", 2);
        first.put("a", 1);
        first.put("items", new LinkedHashSet<>(Arrays.asList("z", "a")));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("items", new LinkedHashSet<>(Arrays.asList("a", "z")));
        second.put("b", 2);

        assertEquals(RepeatSubmitUtils.digestArgs(new Object[]{first}), RepeatSubmitUtils.digestArgs(new Object[]{second}));
    }

    @Test
    void digestArgsShouldDifferWhenPayloadDiffers() {
        assertNotEquals(
                RepeatSubmitUtils.digestArgs(new Object[]{"a", 1}),
                RepeatSubmitUtils.digestArgs(new Object[]{"a", 2})
        );
    }

    @Test
    void digestArgsShouldIgnoreFilteredObjects() {
        String withFiltered = RepeatSubmitUtils.digestArgs(new Object[]{"payload", new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes())});
        String withoutFiltered = RepeatSubmitUtils.digestArgs(new Object[]{"payload"});
        assertEquals(withoutFiltered, withFiltered);
    }

    @Test
    void digestArgsShouldIgnoreBindingResult() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        String withBindingResult = RepeatSubmitUtils.digestArgs(new Object[]{"payload", bindingResult});
        String withoutBindingResult = RepeatSubmitUtils.digestArgs(new Object[]{"payload"});
        assertEquals(withoutBindingResult, withBindingResult);
    }
}
