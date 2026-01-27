package io.ituknown.commons;

public class IdCardUtils {
  /**
   * 校验身份证合法性
   * <p>
   * <a href="https://zh.wikipedia.org/wiki/中华人民共和国公民身份号码">中华人民共和国公民身份号码</a>
   * </p>
   * <p>
   * <a href=
   * "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm">http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm</a>
   * </p>
   */
  private static boolean verify(String cardNum) {
    if (cardNum == null || cardNum.length() != 18) {
      return false;
    }

    int sum = 0;
    for (int idx = 0; idx < 17; ++idx) {
      sum += Math.pow(2, (17 - idx)) % 11 * (cardNum.charAt(idx) - '0');
    }

    int checkCode = (12 - (sum % 11)) % 11;

    if (checkCode < 10) {
      return checkCode == cardNum.charAt(17) - '0';
    } else {
      return cardNum.charAt(17) == 'X' || cardNum.charAt(17) == 'x';
    }
  }
}
