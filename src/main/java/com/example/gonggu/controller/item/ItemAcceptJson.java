package com.example.gonggu.controller.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Optional;
import java.util.OptionalLong;

@Getter
@Setter
@ToString
public class ItemAcceptJson {
    private String a_TabNumber;
    private String a_itemId;
    private String a_userId;
    private Boolean a_editTab;
    private String[] a_imgPathList;

    private String userEmail;

    private String itemCategory;
    private String itemTitle;
    private String itemAmountLimit; // 공구 최소수량
    private String itemNumOfOrder;

    // tab one
    private String oneEndDate;      // 좋아요 받는 마지막 날
    private String oneContents;     // 총대의 메시지
    private String oneLocation;     // 대략적 위치
    //tab two
    private String twoContents;         // 총대의 메시지
    private String twoEndDate;            // 공구 종료시점
    private String twoOptionString;           // 옵션값 한줄로 저장

    // tab3
    private String threeContents;

    //tab four
    private String fourContents;       // 총대의 메시지
    private String fourArrivedTime;      // 도착예정시간
    //tab five
    private String fiveContents;                // 총대의 메시지
    private String fiveLocationDetail;          // 공구물품 배부장소


}