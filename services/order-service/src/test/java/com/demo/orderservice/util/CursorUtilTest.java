package com.demo.orderservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CursorUtilTest {

  @Test
  void encode_ProducesBase64ThatDecodesBackToSameId() {
    assertThat(CursorUtil.decode(CursorUtil.encode(1L))).isEqualTo(1L);
    assertThat(CursorUtil.decode(CursorUtil.encode(42L))).isEqualTo(42L);
    assertThat(CursorUtil.decode(CursorUtil.encode(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void decode_NullOrBlankInput_ReturnsNull(String input) {
    assertThat(CursorUtil.decode(input)).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-valid-base64!@#",
        "e30=" // decodes to {} — no digits, parseLong("") throws
      })
  void decode_InvalidOrUnparseable_ReturnsNull(String input) {
    assertThat(CursorUtil.decode(input)).isNull();
  }
}
