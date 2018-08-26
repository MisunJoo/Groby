package com.example.gonggu.service.item;

import com.example.gonggu.controller.item.JoinAcceptJson;
import com.example.gonggu.domain.category.Category;
import com.example.gonggu.domain.item.*;
import com.example.gonggu.domain.user.User;
import com.example.gonggu.dto.item.*;
import com.example.gonggu.persistence.category.CategoryRepository;
import com.example.gonggu.persistence.item.ItemImgPathRepository;
import com.example.gonggu.persistence.item.ItemRepository;
import com.example.gonggu.persistence.item.ListOfLikeForItemRepo;
import com.example.gonggu.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ListOfLikeForItemRepo likeRepo;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemImgPathRepository itemImgRepo;

    // like 관련 service
    // acceptJson
    //      A_itemId , UserLikeEmail 필수
    // return
    //      true : like 추가
    //      false : like 제거
    @Transactional
    public boolean toggleLike(ItemLikeJson acceptJson){
        Item item = itemRepository.getOne(Long.parseLong(acceptJson.getItemId()));
        boolean result = true;
        Integer likeNum = item.getNumOfLike();
        for(ListOfLikeForItem temp :item.getLikeForItemList())
            if(temp.getUserEmail().equals(acceptJson.getUserEmail()) ){
                item.getLikeForItemList().remove(temp);
                likeRepo.delete(temp.getLikeId());
                result = false; break;
            }

        if(result) {
            ListOfLikeForItem newlike = new ListOfLikeForItem();
            newlike.setUserEmail(acceptJson.getUserEmail());
            item.getLikeForItemList().add(newlike);
            item.setNumOfLike(likeNum + 1);
        }


        return result;
    }

    /*
    * 공구 item 생성관련 Service
    * acceptJson
    *      A_TabNumber, Category, ItemTitle, ItemUserEmail, ItemAmountLimit, a_imgPathList
    *      OneContents, OneLocation, OneEndDate, ""OneImgPath"" <- 이미지 여러개 들어오는 것 처리해야함
    * return
    *      true : create item success
    *      false : create item fail
    *  */
//    public Boolean createItem(ItemAcceptJson acceptJson){
    public Boolean createItem(ItemCreateJson acceptJson){
        Boolean result = true;
        Item item = new Item();
        ItemTab1 itemTab1 = new ItemTab1();
        ItemTab2 itemTab2 = new ItemTab2();
        ItemTab3 itemTab3 = new ItemTab3();
        ItemTab4 itemTab4 = new ItemTab4();
        ItemTab5 itemTab5 = new ItemTab5();
        ItemImgPath itemImgPath;
        List<ItemImgPath> imgPathList = new ArrayList<>();

        // 필수적인 내용을 다 넣어서 보낸 경우 저장
        if((acceptJson.getItemCategory() != null) && (acceptJson.getItemTitle() != null)
                && (acceptJson.getItemAmountLimit() != null) && (acceptJson.getImgPathList() != null)
                && (acceptJson.getTab1().getContents() != null) && (acceptJson.getTab1().getEndDate() != null) && (acceptJson.getTab1().getLocation() != null)) {
            // 공구 item에 대한 기본 설정
            item.setNowTab(1);
            Category getCategory = categoryRepository.findByCategory(acceptJson.getItemCategory());
            item.setCategory(getCategory);
            item.setTitle(acceptJson.getItemTitle());
            User getUser = userRepository.getOne(Long.parseLong(acceptJson.getUserId()));
            item.setUser(getUser);
            item.setAmountLimit(Integer.valueOf(acceptJson.getItemAmountLimit())); // item의 최소공구수량 설정
            item.setNumOfLike(0);
            item.setNumOfOrder(0);
            item.setIsDeleted(false);

            item.setThumnail(acceptJson.getImgPathList()[0]); // 제일 처음에 있는 사진으로 대표이미지 설정

            // 이미지 추가하기
            for (String img : acceptJson.getImgPathList()) {
                itemImgPath = new ItemImgPath();
                itemImgPath.setTab(1);
                itemImgPath.setImg_path(img);
                imgPathList.add(itemImgPath);
            }
            item.setImgPaths(imgPathList); // 이미지리스트 추가하기

            // 공구 item tab1 설정
            itemTab1.setContents(acceptJson.getTab1().getContents());
            itemTab1.setLocation(acceptJson.getTab1().getLocation());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // tab1에서 년월일만 입력받아서 이렇게 설정함
            try {
                Date insertDate = sdf.parse(acceptJson.getTab1().getEndDate());
                itemTab1.setEndDate(insertDate);
            } catch (ParseException e) {
                result = false;
                e.printStackTrace();
            }
            item.setItemTab1(itemTab1);

            // 공구 item tab2,3,4,5 null 값으로 생성
            item.setItemTab2(itemTab2);
            item.setItemTab3(itemTab3);
            item.setItemTab4(itemTab4);
            item.setItemTab5(itemTab5);

            itemRepository.save(item);
        }
        else
            result = false;

        return result;
    }

    /*
    * 공구 tab1에서 아이템 삭제 service
    * parameter
    *   itemId
    * return
    *   true : delete success
    *   false : delete fail
    * */
    public Boolean deleteItem(String itemId) {
        Boolean result = true;

        Item item = itemRepository.findOne(Long.parseLong(itemId));
        if (item.getNowTab() != 1) result = false; // 현재상태가 tab 1이 아닐 경우 삭제불가능하게 처리
        else item.setIsDeleted(true);
        itemRepository.save(item);

        return result;
    }

    /*
    * 공구 item에 대한 모든 정보 반환하는 service
    * parameter
    *   itemId
    * return
    *   itemInfoJson
    * */
    public ItemInfo getItemDetail(String itemId) {
        ItemInfo infoJson = new ItemInfo();
        Item item = itemRepository.findOne(Long.parseLong(itemId));

        if(!item.getIsDeleted()) { // 삭제되지 않은 경우
            infoJson.setItemId(item.getItemId());
            infoJson.setWriterId(item.getUser().getUserId());
            infoJson.setItemTitle(item.getTitle());
            infoJson.setCategory(item.getCategory().getCategory());
            infoJson.setNowTab(item.getNowTab());
            infoJson.setNumOfLike(item.getNumOfLike());
            switch (item.getNowTab()) {
                case 5:
                    ItemTab5Json infoTab5 = new ItemTab5Json();
                    infoTab5.setContents(item.getItemTab5().getContents());
                    infoTab5.setLocationDetail(item.getItemTab5().getLocationDetail());
                    infoJson.setTab5(infoTab5);
                case 4:
                    ItemTab4Json infoTab4 = new ItemTab4Json();
                    infoTab4.setContents(item.getItemTab4().getContents());
                    infoTab4.setArrivedTime(item.getItemTab4().getArrivedTime().toString());
                    infoJson.setTab4(infoTab4);
                case 3:
                    ItemTab3Json infoTab3 = new ItemTab3Json();
                    infoTab3.setContents(item.getItemTab3().getContents());
                    infoJson.setTab3(infoTab3);
                case 2:
                    ItemTab2Json infoTab2 = new ItemTab2Json();
                    infoTab2.setContents(item.getItemTab2().getContents());
                    infoTab2.setEndDate(item.getItemTab2().getEndDate().toString());
                    infoTab2.setOptionString(item.getItemTab2().getOptionString());
                    infoJson.setNumOfParticipant(item.getNumOfOrder());
                    Integer percentage = (item.getNumOfOrder()/item.getAmountLimit())*100;
                    infoJson.setParticipantPercent(percentage);
                    infoJson.setTab2(infoTab2);
                default:
                    ItemTab1Json infoTab1 = new ItemTab1Json();
                    infoTab1.setContents(item.getItemTab1().getContents());
                    infoTab1.setEndDate(item.getItemTab1().getEndDate().toString());
                    infoTab1.setLocation(item.getItemTab1().getLocation());
            }
            infoJson.setImgPathList(item.getImgPaths());
        }
        else infoJson.setIsDeleted(item.getIsDeleted()); // item이 삭제된 경우 삭제되었다는 값만 전달

        return infoJson;
    }

    /*
    * 공구 item의 내용을 수정할때의 service
    * parameter
    *   itemId, info (item_id, 변동된 tab의 내용 필수)
    * return
    *   true : item update success
    *   false : item update fail
    * */
//    public Boolean updateItem(String itemId, ItemAcceptJson info) {
//        Boolean result = true;
//        Item parentsItem = itemRepository.getOne(Long.parseLong(info.getA_itemId()));
//
//        if(info.getItemTitle() != null) parentsItem.setTitle(info.getItemTitle());
//        if(info.getItemAmountLimit() != null) parentsItem.setAmountLimit(Integer.valueOf(info.getItemAmountLimit()));
//        if(Integer.valueOf(info.getA_TabNumber()) != 1) // 탭1일 아닐 경우 수정
//            if(info.getItemNumOfOrder() != null) parentsItem.setNumOfOrder(Integer.valueOf(info.getItemNumOfOrder()));
//        if(info.getItemCategory() != null) { // 카테고리 수정
//            Category getCategory = categoryRepository.findByCategory(info.getItemCategory());
//            parentsItem.setCategory(getCategory);
//        }
//
//        itemRepository.save(parentsItem);
//
//        return result;
//    }

    /*
     * 공구 tab의 내용을 수정할때의 service
     * acceptJson
     *   itemId, a_editTab, 변동된 tab의 내용
     * return
     *   true : item update success
     *   false : item update fail
     * */
    @Transactional
    public Boolean patchItemTab(ItemPatchJson acceptJson){
        Boolean result = true;
        Item parentsItem = itemRepository.getOne(acceptJson.getItemId());
        this.changeTabImgs(Integer.valueOf(acceptJson.getTargetTab()) , acceptJson.getImgPathList() ,parentsItem);

        switch (acceptJson.getTargetTab().toString()){
            case "1" :
                ItemTab1 tab1 = parentsItem.getItemTab1();
                if(acceptJson.getEditTab()) {
                    if(acceptJson.getItemTitle() != null) parentsItem.setTitle(acceptJson.getItemTitle());
                    if(acceptJson.getItemAmountLimit() != null) parentsItem.setAmountLimit(acceptJson.getItemAmountLimit());
                    if(acceptJson.getCategory() != null) {
                        Category getCategory = categoryRepository.findByCategory(acceptJson.getCategory());
                        parentsItem.setCategory(getCategory);
                    }
                    if(acceptJson.getTab1().getContents() != null) tab1.setContents(acceptJson.getTab1().getContents());
                    if(acceptJson.getTab1().getLocation() != null) tab1.setLocation(acceptJson.getTab1().getLocation());
                    if(acceptJson.getTab1().getEndDate() != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // tab1에서 년월일만 입력받아서 이렇게 설정함
                        try {
                            Date newEndDate = sdf.parse(acceptJson.getTab1().getEndDate());
                            tab1.setEndDate(newEndDate);
                        } catch (ParseException e) {
                            result = false;
                            e.printStackTrace();
                        }
                    }
                    parentsItem.setItemTab1(tab1);
                }
                break;
            case "2" :
                // optionString contents img_path
                ItemTab2 tab2 = parentsItem.getItemTab2();
                if(acceptJson.getTab2().getContents() != null) tab2.setContents(acceptJson.getTab2().getContents());
                if(acceptJson.getTab2().getOptionString() != null) tab2.setOptionString(acceptJson.getTab2().getOptionString());
                if(acceptJson.getTab2().getEndDate() != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        Date twoEndDate = format.parse(acceptJson.getTab2().getEndDate());
                        tab2.setEndDate(twoEndDate);
                    } catch (ParseException e) {
                        result = false;
                        e.printStackTrace();
                    }
                }
                parentsItem.setItemTab2(tab2);
                break;
            case "3" :
                ItemTab3 tab3 = parentsItem.getItemTab3();
                if(acceptJson.getTab3().getContents() != null) tab3.setContents(acceptJson.getTab3().getContents());
            case "4" :
                ItemTab4 tab4 = parentsItem.getItemTab4();
                if(acceptJson.getTab4().getArrivedTime() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // tab1에서 년월일만 입력받아서 이렇게 설정함
                    try {
                        Date newArriveDate = sdf.parse(acceptJson.getTab4().getArrivedTime());
                        tab4.setArrivedTime(newArriveDate);
                    } catch (ParseException e) {
                        result = false;
                        e.printStackTrace();
                    }
                }
                if(acceptJson.getTab4().getContents() != null) tab4.setContents(acceptJson.getTab4().getContents());
                parentsItem.setItemTab4(tab4);
                break;
            case "5" :
                ItemTab5 tab5 = parentsItem.getItemTab5();
                if(acceptJson.getTab5().getLocationDetail() != null) tab5.setLocationDetail(acceptJson.getTab5().getLocationDetail());
                if(acceptJson.getTab5().getContents() != null) tab5.setContents(acceptJson.getTab5().getContents());
                parentsItem.setItemTab5(tab5);
                break;
        }

        if(!acceptJson.getEditTab()) {
            parentsItem.setNowTab(Integer.valueOf(acceptJson.getTargetTab()));
        }
        itemRepository.save(parentsItem);
        return result;
    }

    /*
     * img list change
     * parameter
     *   tab , new_imgpaths , target Item
     * return
     *   void
     * */
    public void changeTabImgs(Integer tab,String[] imgpaths,Item target){

        List<ItemImgPath> list = target.getImgPaths().stream().filter(img->{
            if (img.getTab() == tab) {
                itemImgRepo.delete(img.getItemImgPathId());
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        for (String newImgString : imgpaths) {
            ItemImgPath newImgPath = new ItemImgPath();
            newImgPath.setTab(tab);
            newImgPath.setImg_path(newImgString);
            list.add(newImgPath);
        }

        target.setImgPaths(list);
    }

    /*
     * 사용자가 공구 구매시 option에 대한 정보 전달 service
     * parameter
     *   itemId
     * return
     *   String : option에 대한 값 전달
     * */
    public String getOptionInfo(String itemId) {
        String result;
        Item parentItem = itemRepository.getOne(Long.parseLong(itemId));

        result = parentItem.getItemTab2().getOptionString();

        return result;
    }

    /*
     * 유저의 공구 참여
     * parameter
     *   acceptJson
     * return
     *   boolean
     * */
    public boolean insertParticipantUser(JoinAcceptJson acceptJson){
        boolean result = true;

        Item parentItem = itemRepository.getOne(Long.parseLong(acceptJson.getItemId()));
        ListOfParticipantForItem join = new ListOfParticipantForItem();
        join.setUserName(acceptJson.getUserName());
        join.setAccountBank(acceptJson.getAccountBank());
        join.setAccountHolder(acceptJson.getAccountHolder());
        join.setAccountNum(acceptJson.getAccountNum());
        join.setOptionString(acceptJson.getOptionString());
        join.setPrice(acceptJson.getPrice());
        join.setAmount(acceptJson.getAmount());
        List<ListOfParticipantForItem> list = parentItem.getParticipantForItemList();
        list.add(join);
        parentItem.setParticipantForItemList(list);
        itemRepository.save(parentItem);

        return result;
    }
}
