package com.meetbowl.domain.meetingroom;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 사이트(거점) 기준정보 도메인 모델이다.
 *
 * <p>회의실 기준정보 계층 Site → Building → MeetingRoom 의 최상위다. 관리자가 회의실 등록 화면에서 별도로 추가할 수 있다(FR-095). 불변
 * 객체로 다루며 변경은 새 인스턴스를 반환한다.
 */
public class Site {

    private final UUID id;

    /** 사이트명(필수). */
    private final String name;

    /** 주소(선택). */
    private final String address;

    private Site(UUID id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public static Site create(String name, String address) {
        return of(null, name, address);
    }

    public static Site of(UUID id, String name, String address) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사이트명은 필수입니다.");
        }
        return new Site(id, name, address);
    }

    public Site change(String newName, String newAddress) {
        return of(id, newName, newAddress);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }
}
