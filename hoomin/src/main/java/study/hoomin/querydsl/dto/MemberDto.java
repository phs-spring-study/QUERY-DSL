package study.hoomin.querydsl.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MemberDto {
	private String userName;
	private int age;

	public MemberDto(String userName, int age) {
		this.userName = userName;
		this.age = age;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
