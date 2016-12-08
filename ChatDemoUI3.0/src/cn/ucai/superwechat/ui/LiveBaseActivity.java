package cn.ucai.superwechat.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.florent37.viewanimator.AnimationListener;
import com.github.florent37.viewanimator.ViewAnimator;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMChatRoomChangeListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.bean.Result;
import cn.ucai.superwechat.data.NetDao;
import cn.ucai.superwechat.data.OkHttpUtils;
import cn.ucai.superwechat.domain.Gift;
import cn.ucai.superwechat.domain.Wallet;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.L;
import cn.ucai.superwechat.utils.ResultUtils;
import cn.ucai.superwechat.utils.Utils;
import cn.ucai.superwechat.widget.BarrageLayout;
import cn.ucai.superwechat.widget.LiveLeftGiftView;
import cn.ucai.superwechat.widget.PeriscopeLayout;
import cn.ucai.superwechat.widget.RoomMessagesView;

/**
 * Created by wei on 2016/6/12.
 */
public abstract class LiveBaseActivity extends BaseActivity {
  protected static final String TAG = "LiveActivity";

  @BindView(R.id.left_gift_view1)
  LiveLeftGiftView leftGiftView;
  @BindView(R.id.left_gift_view2) LiveLeftGiftView leftGiftView2;
  @BindView(R.id.message_view)
  RoomMessagesView messageView;
  @BindView(R.id.periscope_layout)
  PeriscopeLayout periscopeLayout;
  @BindView(R.id.bottom_bar) View bottomBar;

  @BindView(R.id.barrage_layout)
  BarrageLayout barrageLayout;
  @BindView(R.id.horizontal_recycle_view)
  RecyclerView horizontalRecyclerView;
  @BindView(R.id.audience_num) TextView audienceNumView;
  @BindView(R.id.new_messages_warn) ImageView newMsgNotifyImage;

  protected String anchorId;

  /**
   * 环信聊天室id
   */
  protected String chatroomId = "";
  /**
   * ucloud直播id
   */
  protected String liveId = "";
  protected boolean isMessageListInited;
  protected EMChatRoomChangeListener chatRoomChangeListener;

  volatile boolean isGiftShowing = false;
  volatile boolean isGift2Showing = false;
  List<EMMessage> toShowList = Collections.synchronizedList(new LinkedList<EMMessage>());

  protected EMChatRoom chatroom;
  List<String> memberList = new ArrayList<>();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onActivityCreate(savedInstanceState);
  }

  protected Handler handler = new Handler();

  protected abstract void onActivityCreate(@Nullable Bundle savedInstanceState);

  protected synchronized void showLeftGiftVeiw(EMMessage message) {
    try {
      String msgId = message.getMsgId();
      final String nick = message.getStringAttribute(I.User.NICK);
      final String username = message.getFrom();
      final int giftId = message.getIntAttribute(Constant.CMD_GIFT_TYPE);
      if (!isGift2Showing) {
        showGift2Derect(message);
      } else if (!isGiftShowing) {
        showGift1Derect(message);
      } else {
        toShowList.add(message);
      }
    }catch (Exception e){
      L.e(TAG,"e="+e.toString());
    }
  }

  private void showGift1Derect(final EMMessage message) throws HyphenateException {
    final String nick = message.getStringAttribute(I.User.NICK);
    final String username = message.getFrom();
    final int giftId = message.getIntAttribute(Constant.CMD_GIFT_TYPE);
    isGiftShowing = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        leftGiftView.setVisibility(View.VISIBLE);
        leftGiftView.setName(nick);
        leftGiftView.setAvatar(username);
        leftGiftView.setTranslationY(0);
        leftGiftView.setGift(giftId);
        ViewAnimator.animate(leftGiftView)
            .alpha(0, 1)
            .translationX(-leftGiftView.getWidth(), 0)
            .duration(600)
            .thenAnimate(leftGiftView)
            .alpha(1, 0)
            .translationY(-1.5f * leftGiftView.getHeight())
            .duration(800)
            .onStop(new AnimationListener.Stop() {
              @Override public void onStop() {
                EMMessage pollName = null;
                try {
                  if(toShowList.size()>0) {
                    pollName = toShowList.remove(0);
                    if (pollName != null) {
                      showGift1Derect(pollName);
                    } else {
                      isGiftShowing = false;
                    }
                  }else {
                    isGiftShowing = false;
                  }
                } catch (Exception e) {
                  L.e(TAG,"showGift1Derect,e="+e.toString());
                  isGift2Showing = false;
                }
              }
            })
            .startDelay(200)
            .start();
        ViewAnimator.animate(leftGiftView.getGiftImageView())
            .translationX(-leftGiftView.getGiftImageView().getX(), 0)
            .duration(1100)
            .start();
      }
    });
  }

  private void showGift2Derect(final EMMessage message) throws HyphenateException {
    final String nick2 = message.getStringAttribute(I.User.NICK);
    final String username2 = message.getFrom();
    final int giftId2 = message.getIntAttribute(Constant.CMD_GIFT_TYPE);
    isGift2Showing = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        leftGiftView2.setVisibility(View.VISIBLE);
        leftGiftView2.setName(nick2);
        leftGiftView2.setAvatar(username2);
        leftGiftView2.setTranslationY(0);
        leftGiftView2.setGift(giftId2);
        ViewAnimator.animate(leftGiftView2)
            .alpha(0, 1)
            .translationX(-leftGiftView2.getWidth(), 0)
            .duration(600)
            .thenAnimate(leftGiftView2)
            .alpha(1, 0)
            .translationY(-1.5f * leftGiftView2.getHeight())
            .duration(800)
            .onStop(new AnimationListener.Stop() {
              @Override public void onStop() {
                EMMessage pollName2 = null;
                try {
                  if(toShowList.size()>0) {
                    pollName2 = toShowList.remove(0);
                    if (pollName2 != null) {
                      showGift2Derect(pollName2);
                    } else {
                      isGift2Showing = false;
                    }
                  } else {
                    isGift2Showing = false;
                  }
                } catch (Exception e) {
                  L.e(TAG,"showGift2Derect,e="+e.toString());
                  isGift2Showing = false;
                }
              }
            })
            .startDelay(200)
            .start();
        ViewAnimator.animate(leftGiftView2.getGiftImageView())
            .translationX(-leftGiftView2.getGiftImageView().getX(), 0)
            .duration(1100)
            .start();
      }
    });
  }

  protected void addChatRoomChangeListenr() {
    chatRoomChangeListener = new EMChatRoomChangeListener() {

      @Override public void onChatRoomDestroyed(String roomId, String roomName) {
        if (roomId.equals(chatroomId)) {
          EMLog.e(TAG, " room : " + roomId + " with room name : " + roomName + " was destroyed");
        }
      }

      @Override public void onMemberJoined(String roomId, String participant) {
        EMMessage message = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
        message.setReceipt(chatroomId);
        message.setFrom(participant);
        EMTextMessageBody textMessageBody = new EMTextMessageBody("来了");
        message.addBody(textMessageBody);
        message.setChatType(EMMessage.ChatType.ChatRoom);
        EMClient.getInstance().chatManager().saveMessage(message);
        messageView.refreshSelectLast();

        onRoomMemberAdded(participant);
      }

      @Override public void onMemberExited(String roomId, String roomName, String participant) {
        //                showChatroomToast("member : " + participant + " leave the room : " + roomId + " room name : " + roomName);
        onRoomMemberExited(participant);
      }

      @Override public void onMemberKicked(String roomId, String roomName, String participant) {
        if (roomId.equals(chatroomId)) {
          String curUser = EMClient.getInstance().getCurrentUser();
          if (curUser.equals(participant)) {
            EMClient.getInstance().chatroomManager().leaveChatRoom(roomId);
            CommonUtils.showShortToast("你已被移除出此房间");
            finish();
          } else {
            //                        showChatroomToast("member : " + participant + " was kicked from the room : " + roomId + " room name : " + roomName);
            onRoomMemberExited(participant);
          }
        }
      }
    };

    EMClient.getInstance().chatroomManager().addChatRoomChangeListener(chatRoomChangeListener);
  }

  EMMessageListener msgListener = new EMMessageListener() {

    @Override public void onMessageReceived(List<EMMessage> messages) {

      for (EMMessage message : messages) {
        String username = null;
        // 群组消息
        if (message.getChatType() == EMMessage.ChatType.GroupChat
            || message.getChatType() == EMMessage.ChatType.ChatRoom) {
          username = message.getTo();
        } else {
          // 单聊消息
          username = message.getFrom();
        }
        // 如果是当前会话的消息，刷新聊天页面
        if (username.equals(chatroomId)) {
          if (message.getBooleanAttribute(Constant.EXTRA_IS_BARRAGE_MSG, false)) {
            barrageLayout.addBarrage(((EMTextMessageBody) message.getBody()).getMessage(),
                message.getFrom());
          }
          messageView.refreshSelectLast();
        } else {
          if(message.getChatType() == EMMessage.ChatType.Chat && message.getTo().equals(EMClient.getInstance().getCurrentUser())){
            runOnUiThread(new Runnable() {
              @Override public void run() {
                newMsgNotifyImage.setVisibility(View.VISIBLE);
              }
            });
          }
          //// 如果消息不是和当前聊天ID的消息
          //EaseUI.getInstance().getNotifier().onNewMsg(message);
        }
      }
    }

    @Override public void onCmdMessageReceived(List<EMMessage> messages) {
      final EMMessage message = messages.get(messages.size() - 1);
      if (Constant.CMD_GIFT.equals(((EMCmdMessageBody) message.getBody()).action())) {
        L.e(TAG,"onCmdMessageReceived,giftId="+message);
        showLeftGiftVeiw(message);
//        NetDao.searchUser(LiveBaseActivity.this, message.getFrom(), new OkHttpUtils.OnCompleteListener<String>() {
//          @Override
//          public void onSuccess(String s) {
//            if(s!=null){
//              Result result = ResultUtils.getResultFromJson(s, User.class);
//              if(result!=null && result.isRetMsg()){
//                User u = (User) result.getRetData();
//                if(u!=null && u.getMUserNick()!=null) {
//                  showLeftGiftVeiw(u.getMUserNick(),message.getFrom(),giftId);
//                }else{
//                  showLeftGiftVeiw(message.getFrom(),message.getFrom(),giftId);
//                }
//              }
//            }
//          }
//
//          @Override
//          public void onError(String error) {
//            showLeftGiftVeiw(message.getFrom(),message.getFrom(),giftId);
//          }
//        });
      }
    }

    @Override public void onMessageReadAckReceived(List<EMMessage> messages) {
      if (isMessageListInited) {
        //                messageList.refresh();
      }
    }

    @Override public void onMessageDeliveryAckReceived(List<EMMessage> message) {
      if (isMessageListInited) {
        //                messageList.refresh();
      }
    }

    @Override public void onMessageChanged(EMMessage message, Object change) {
      if (isMessageListInited) {
        messageView.refresh();
      }
    }
  };

  protected void onMessageListInit() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        messageView.init(chatroomId);
        messageView.setMessageViewListener(new RoomMessagesView.MessageViewListener() {
          @Override public void onMessageSend(String content) {
            EMMessage message = EMMessage.createTxtSendMessage(content, chatroomId);
            if (messageView.isBarrageShow) {
              message.setAttribute(Constant.EXTRA_IS_BARRAGE_MSG, true);
              barrageLayout.addBarrage(content, EMClient.getInstance().getCurrentUser());
            }
            message.setChatType(EMMessage.ChatType.ChatRoom);
            EMClient.getInstance().chatManager().sendMessage(message);
            message.setMessageStatusCallback(new EMCallBack() {
              @Override public void onSuccess() {
                //刷新消息列表
                messageView.refreshSelectLast();
              }

              @Override public void onError(int i, String s) {
                CommonUtils.showShortToast("消息发送失败！");
              }

              @Override public void onProgress(int i, String s) {

              }
            });
          }

          @Override public void onItemClickListener(final EMMessage message) {
            //if(message.getFrom().equals(EMClient.getInstance().getCurrentUser())){
            //    return;
            //}
            String clickUsername = message.getFrom();
            showUserDetailsDialog(clickUsername);
          }

          @Override public void onHiderBottomBar() {
            bottomBar.setVisibility(View.VISIBLE);
          }
        });
        messageView.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        isMessageListInited = true;
        updateUnreadMsgView();
        showMemberList();
      }
    });
  }

  protected void updateUnreadMsgView(){
    if(isMessageListInited) {
      for (EMConversation conversation : EMClient.getInstance()
          .chatManager()
          .getAllConversations()
          .values()) {
        if (conversation.getType() == EMConversation.EMConversationType.Chat
            && conversation.getUnreadMsgCount() > 0) {
          newMsgNotifyImage.setVisibility(View.VISIBLE);
          return;
        }
      }
      newMsgNotifyImage.setVisibility(View.INVISIBLE);
    }
  }


  private void showUserDetailsDialog(String username) {
    final RoomUserDetailsDialog dialog =
        RoomUserDetailsDialog.newInstance(username);
    dialog.setUserDetailsDialogListener(
        new RoomUserDetailsDialog.UserDetailsDialogListener() {
          @Override public void onMentionClick(String username) {
            dialog.dismiss();
            messageView.getInputView().setText("@" + username + " ");
            showInputView();
          }
        });
    dialog.show(getSupportFragmentManager(), "RoomUserDetailsDialog");
  }

  private void showInputView() {
    bottomBar.setVisibility(View.INVISIBLE);
    messageView.setShowInputView(true);
    messageView.getInputView().requestFocus();
    messageView.getInputView().requestFocusFromTouch();
    handler.postDelayed(new Runnable() {
      @Override public void run() {
        Utils.showKeyboard(messageView.getInputView());
      }
    }, 200);
  }

  void showMemberList() {
    LinearLayoutManager layoutManager = new LinearLayoutManager(LiveBaseActivity.this);
    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
    horizontalRecyclerView.setLayoutManager(layoutManager);
    horizontalRecyclerView.setAdapter(new AvatarAdapter(LiveBaseActivity.this, memberList));
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          chatroom =
              EMClient.getInstance().chatroomManager().fetchChatRoomFromServer(chatroomId, true);
          memberList.addAll(chatroom.getMemberList());
        } catch (HyphenateException e) {
          e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
          @Override public void run() {
            audienceNumView.setText(String.valueOf(memberList.size()));
            horizontalRecyclerView.getAdapter().notifyDataSetChanged();
          }
        });
      }
    }).start();
  }

  private void onRoomMemberAdded(String name) {
    if (!memberList.contains(name)) memberList.add(name);
    runOnUiThread(new Runnable() {
      @Override public void run() {
        audienceNumView.setText(String.valueOf(memberList.size()));
        horizontalRecyclerView.getAdapter().notifyDataSetChanged();
      }
    });
  }

  private void onRoomMemberExited(String name) {
    memberList.remove(name);
    runOnUiThread(new Runnable() {
      @Override public void run() {
        audienceNumView.setText(String.valueOf(memberList.size()));
        horizontalRecyclerView.getAdapter().notifyDataSetChanged();
      }
    });
  }

  @OnClick(R.id.root_layout) void onRootLayoutClick() {
    periscopeLayout.addHeart();
  }

  @OnClick(R.id.comment_image) void onCommentImageClick() {
    showInputView();
  }

//  @OnClick(R.id.present_image) void onPresentImageClick() {
//    EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
//    message.setReceipt(chatroomId);
//    EMCmdMessageBody cmdMessageBody = new EMCmdMessageBody(Constant.CMD_GIFT);
//    message.addBody(cmdMessageBody);
//    message.setChatType(EMMessage.ChatType.ChatRoom);
//    EMClient.getInstance().chatManager().sendMessage(message);
//    showLeftGiftVeiw(SuperWeChatHelper.getInstance().getAppContactList()
//            .get(EMClient.getInstance().getCurrentUser()).getMUserNick(),
//            EMClient.getInstance().getCurrentUser());
//  }

  @OnClick(R.id.chat_image) void onChatImageClick() {
    ConversationListFragment fragment = ConversationListFragment.newInstance(anchorId, false);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.message_container, fragment)
        .commit();
  }

  @OnClick(R.id.present_image) void onPresentImageClick() {
    final RoomGiftListDialog dialog =
            RoomGiftListDialog.newInstance("");
    dialog.setGiftOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                showPaymentTips(dialog,v);
              }
            }
    );
    dialog.show(getSupportFragmentManager(), "RoomGiftListDialog");
  }

  private void showPaymentTips(final RoomGiftListDialog dialog, final View v){
    if(!SuperWeChatHelper.getInstance().getUserProfileManager().getPaymentTips()) {
      int giftId = (int) v.getTag();
      Gift gift = SuperWeChatHelper.getInstance().getGiftList().get(giftId);
      if(gift!=null) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(LiveBaseActivity.this);
        builder.setTitle("提示");
        builder.setMessage("需要消费微信币:"+gift.getGprice()+",确认支付么?");
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.payment_tips, null);
        CheckBox cb = (CheckBox) linearLayout.findViewById(R.id.payment_tips_nomore);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            L.e(TAG,"set payment tips is "+isChecked);
            SuperWeChatHelper.getInstance().getUserProfileManager().setPaymentTips(isChecked);
          }
        });
        builder.setView(linearLayout);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface d, int which) {
            sendGift(dialog,v);
          }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        });

        builder.create().show();
      }
    }else{
      sendGift(dialog,v);
    }
  }

  private void sendGift(final RoomGiftListDialog dialog, final View v) {
    int giftId = (int) v.getTag();
    Gift gift = SuperWeChatHelper.getInstance().getGiftList().get(giftId);
    String username = EMClient.getInstance().getCurrentUser();
    if(gift!=null) {
      int change = SuperWeChatHelper.getInstance().getUserProfileManager().getCurrentUserChange();
      if(change>=gift.getGprice()){
        //扣钱
        NetDao.givingGift(LiveBaseActivity.this,username,chatroom.getOwner(),giftId,
                new OkHttpUtils.OnCompleteListener<String>() {
                  @Override
                  public void onSuccess(String s) {
                    if(s!=null){
                      Result result = ResultUtils.getResultFromJson(s, Wallet.class);
                      if(result!=null && result.isRetMsg()){
                        Wallet wallet = (Wallet) result.getRetData();
                        if(wallet!=null) {
                          SuperWeChatHelper.getInstance().getUserProfileManager().setCurrentUserChange(wallet.getBalance());
                          sendGiftShowMsg(dialog,v);
                        }else{
                        }
                      }else{
                      }
                    }
                  }

                  @Override
                  public void onError(String error) {
                  }
                });
      }else{
        final AlertDialog.Builder builder = new AlertDialog.Builder(LiveBaseActivity.this);
        builder.setTitle("提示");
        builder.setMessage("余额不足,去充值吧!");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        });
        builder.create().show();
      }
    }

  }

  private void sendGiftShowMsg(final RoomGiftListDialog dialog, final View v) {
    String username = EMClient.getInstance().getCurrentUser();
    int giftId = (int) v.getTag();
    //发送消息
    dialog.dismiss();
    String nick = SuperWeChatHelper.getInstance().getAppContactList()
            .get(username).getMUserNick();
    EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
    message.setReceipt(chatroomId);
    EMCmdMessageBody cmdMessageBody = new EMCmdMessageBody(Constant.CMD_GIFT);
    message.addBody(cmdMessageBody);
    message.setAttribute(Constant.CMD_GIFT_TYPE, giftId);
    message.setAttribute(I.User.NICK, nick);
    message.setAttribute(I.User.USER_NAME, username);
    message.setChatType(EMMessage.ChatType.ChatRoom);
    EMClient.getInstance().chatManager().sendMessage(message);
    showLeftGiftVeiw(message);
  }


  @OnClick(R.id.screenshot_image) void onScreenshotImageClick(){
    Bitmap bitmap = screenshot();
    if (bitmap != null) {
      ScreenshotDialog dialog = new ScreenshotDialog(this, bitmap);
      dialog.show();
    }

  }

  private Bitmap screenshot()
  {
    // 获取屏幕
    View dView = getWindow().getDecorView();
    dView.setDrawingCacheEnabled(true);
    dView.buildDrawingCache();
    Bitmap bmp = dView.getDrawingCache();
    return bmp;
  }

  @Override protected void onResume() {
    super.onResume();
  }

  private class AvatarAdapter extends RecyclerView.Adapter<AvatarViewHolder> {
    List<String> namelist;
    Context context;
//    TestAvatarRepository avatarRepository;

    public AvatarAdapter(Context context, List<String> namelist) {
      this.namelist = namelist;
      this.context = context;
//      avatarRepository = new TestAvatarRepository();
    }

    @Override public AvatarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new AvatarViewHolder(
          LayoutInflater.from(context).inflate(R.layout.avatar_list_item, parent, false));
    }

    @Override public void onBindViewHolder(AvatarViewHolder holder, final int position) {
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          showUserDetailsDialog(namelist.get(position));
        }
      });

      EaseUserUtils.setAppUserAvatar(context,namelist.get(position),holder.Avatar);
      //暂时使用测试数据
//      Glide.with(context)
//          .load(avatarRepository.getAvatar())
//          .placeholder(R.drawable.ease_default_avatar)
//          .into(holder.Avatar);
    }

    @Override public int getItemCount() {
      return namelist.size();
    }
  }

  static class AvatarViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.avatar) ImageView Avatar;

    public AvatarViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}