package com.feedbacker.member.dto;

/**
 * 4-4 회원 탈퇴 요청.
 * 이메일 가입자는 비밀번호 필수, 카카오 가입자는 비밀번호가 없으므로 생략
 */
public record WithdrawRequest(String password) {
}
