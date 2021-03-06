package com.example.gonggu.service.user;

import com.example.gonggu.domain.item.Item;
import com.example.gonggu.dto.user.*;
import com.example.gonggu.domain.user.User;
import com.example.gonggu.dto.view.ItemCard;
import com.example.gonggu.exception.NotFoundException;
import com.example.gonggu.persistence.item.ItemRepository;
import com.example.gonggu.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepo;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private MailSender sender;

    // 해당 유저를 찾아서 리턴해준다.
    // info 의 getUserBy Key를 통해서 메서드를 변경한다.
    public UserInfo getUserBy(Map<String,Object> acceptMap){
        User user;
        if(acceptMap.get("getUserBy").toString().equals("Email")){
            user = userRepository.findByUserEmail(acceptMap.get("userEmail").toString());
        }else{
            user = userRepository.findOne(Long.parseLong(acceptMap.get("userId").toString()));
        }
        UserInfo result = new UserInfo();
        result.setUserId(user.getUserId().toString());
        result.setAccountBank(user.getAccountBank());
        result.setAccountHolder(user.getAccountHolder());
        result.setAccountNum(user.getAccountNum());
        result.setIsDeleted(user.getIsDeleted());
        result.setUserEmail(user.getUserEmail());
        result.setUserName(user.getUserName());

        return result;
    }

    public void createUser(UserSignupJson acceptJson) {
        User user = new User();
        user.setUserEmail(acceptJson.getUserEmail());
        user.setUserPw(bCryptPasswordEncoder.encode(acceptJson.getUserPw()));
        user.setUserName(acceptJson.getUserName());
        user.setUserToken(acceptJson.getUserToken());

        // signup 단계에서 계좌 정보를 받지 않음
//        if(info.get("userAccountBank") != null){
//            user.setAccountBank(info.get("userAccountBank").toString());
//            user.setAccountHolder(info.get("userAccountHolder").toString());
//            user.setAccountNum(info.get("userAccountNum").toString());
//        }

        userRepository.save(user);
    }

    public void userUpdate(UserPatchJson acceptJson) {
        User user = userRepository.findByUserEmail(acceptJson.getUserEmail());
        if(user == null) throw new NotFoundException("유저가 존재하지 않습니다.");

        if(acceptJson.getUserName() != null) user.setUserName(acceptJson.getUserName());
        if(acceptJson.getAccountBank() != null) user.setAccountBank(acceptJson.getAccountBank());
        if(acceptJson.getAccountHolder() != null) user.setAccountHolder(acceptJson.getAccountHolder());
        if(acceptJson.getAccountNum() != null) user.setAccountNum(acceptJson.getAccountNum());
        if(acceptJson.getPhoneNumber() != null) user.setPhoneNum(acceptJson.getPhoneNumber());
        if(acceptJson.getUserToken() != null) user.setUserToken(acceptJson.getUserToken());

        userRepository.save(user);
    }

    public void userSetPassword(UserPwJson acceptJson){
        User user = userRepository.findByUserEmail(acceptJson.getUserEmail());
        if(user == null) throw new NotFoundException("유저가 존재하지 않습니다.");
        user.setUserPw(acceptJson.getUserPw());

        userRepository.save(user);
    }


    public UserInfo loginUser(UserLoginJson acceptJson) {
        User checkUser = userRepository.findByUserEmail(acceptJson.getUserEmail());
        if (checkUser == null) throw new NotFoundException("유저가 존재하지 않습니다.");

        UserInfo result = null;

        if (bCryptPasswordEncoder.matches(acceptJson.getUserPw(),checkUser.getUserPw())) {
            result = new UserInfo();
            result.setUserId(checkUser.getUserId().toString());
            result.setUserName(checkUser.getUserName());
            result.setUserEmail(checkUser.getUserEmail());
            result.setAccountBank(checkUser.getAccountBank());
            result.setAccountHolder(checkUser.getAccountHolder());
            result.setAccountNum(checkUser.getAccountNum());
            result.setIsDeleted(checkUser.getIsDeleted());
            result.setPhoneNumber(checkUser.getPhoneNum());
        }

        return result;

    }


    public void serviceDeleteUser(String userId){
        User checkUser = userRepository.getOne(Long.valueOf(userId));
        if (checkUser == null) throw new NotFoundException("유저가 존재하지 않습니다.");

        checkUser.setIsDeleted(true);

        userRepository.save(checkUser);
    }

    // for Developer not for Service
    public boolean deleteUser(String userId) {
        User checkUser = userRepository.getOne(Long.valueOf(userId));

        if(checkUser == null){
            return false;
        }

        userRepository.delete(checkUser);
        return true;
    }

    public boolean checkEmail(String userEmail){
        if(userRepository.findByUserEmail(userEmail) == null){
            return true;
        }else
            return false;
    }


    public String sendMail(String userEmail) {
        String key = new TempKey().getKey(4, true);

        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom("mashupdutchmarket@gmail.com");
        msg.setTo(userEmail);
        msg.setSubject("Groby 인증번호");
        msg.setText("Groby 에서 인증 메일을 보냅니다. \n\""+key+ "\" 를 앱에서 입력해주세요 \n By Groby");

        this.sender.send(msg);

        return key;
    }

    public List<ItemCard> getUserParticipantList(boolean owner, String userId){
        User usr = userRepository.getOne(Long.parseLong(userId));
        List<ItemCard> returnList = new ArrayList<>();

        usr.getParticipants().forEach(participant->{  if(participant.getOwner() == owner){
            ItemCard temp = new ItemCard();
            Item tempSource = itemRepo.findByListOfParticipantForUser(participant);
            temp.setThumnailURL(tempSource.getThumnail());
            temp.setNowTab(tempSource.getNowTab());
            temp.setItemId(tempSource.getItemId().toString());
            temp.setTitle(tempSource.getTitle());
            switch (tempSource.getNowTab()) {
                case 1:  // Tab1인 경우
                    temp.setDueDate(tempSource.getItemTab1().getEndDate().toString());
                    temp.setLikeNum(tempSource.getNumOfLike().toString());
                    break;
                case 2:  // Tab2인 경우
                    temp.setDueDate(tempSource.getItemTab2().getEndDate().toString());
                    temp.setAmountLimit(tempSource.getAmountLimit());                 // 공구주문까지 최소수량
                    temp.setParticipantNum(tempSource.getNumOfOrder());               // 지금까지 구매된 수량
                    Integer percentage = (tempSource.getNumOfOrder()/tempSource.getAmountLimit())*100;
                    temp.setParticipantPercent(percentage);                              // 진행률
                    break;
                default:
                    break;
            }

            returnList.add(temp);
            }
        });

        return returnList;
    }

}