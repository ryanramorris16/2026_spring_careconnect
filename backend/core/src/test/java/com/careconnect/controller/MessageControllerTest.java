package com.careconnect.controller;

import com.careconnect.dto.InboxMessageDto;
import com.careconnect.model.Message;
import com.careconnect.model.User;
import com.careconnect.repository.MessageRepository;
import com.careconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock private MessageRepository messageRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private MessageController controller;

    private static final Long SENDER_ID   = 1L;
    private static final Long RECEIVER_ID = 2L;

    private Message makeMessage(Long id, Long senderId, Long receiverId, String content) {
        Message m = new Message();
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setContent(content);
        m.setTimestamp(LocalDateTime.now());
        m.setRead(false);
        return m;
    }

    private User makeUser(Long id, String name, String email) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(email);
        return u;
    }

    // ─── sendMessage ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_setsTimestampAndIsRead_thenSaves() {
        Message inbound = new Message();
        inbound.setSenderId(SENDER_ID);
        inbound.setReceiverId(RECEIVER_ID);
        inbound.setContent("Hello!");

        Message saved = makeMessage(1L, SENDER_ID, RECEIVER_ID, "Hello!");
        when(messageRepo.save(any(Message.class))).thenReturn(saved);

        ResponseEntity<Message> response = controller.sendMessage(inbound);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
        // verify timestamp and isRead were set before save
        verify(messageRepo).save(argThat(m -> m.getTimestamp() != null && !m.isRead()));
    }

    // ─── getConversation ──────────────────────────────────────────────────────

    @Test
    void getConversation_returns200_withMessages() {
        Message msg = makeMessage(1L, SENDER_ID, RECEIVER_ID, "Hi");
        when(messageRepo.findConversation(SENDER_ID, RECEIVER_ID)).thenReturn(List.of(msg));

        ResponseEntity<List<Message>> response = controller.getConversation(SENDER_ID, RECEIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getConversation_returns200_emptyConversation() {
        when(messageRepo.findConversation(SENDER_ID, RECEIVER_ID)).thenReturn(List.of());

        ResponseEntity<List<Message>> response = controller.getConversation(SENDER_ID, RECEIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── getInbox ─────────────────────────────────────────────────────────────

    @Test
    void getInbox_noMessages_returnsEmptyList() {
        when(messageRepo.findAllUserMessages(SENDER_ID)).thenReturn(List.of());

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(SENDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getInbox_userIsSender_peerIsReceiver() {
        Message msg = makeMessage(1L, SENDER_ID, RECEIVER_ID, "Hey");
        when(messageRepo.findAllUserMessages(SENDER_ID)).thenReturn(List.of(msg));
        User peer = makeUser(RECEIVER_ID, "Bob", "bob@test.com");
        when(userRepo.findById(RECEIVER_ID)).thenReturn(Optional.of(peer));

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(SENDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        InboxMessageDto dto = response.getBody().get(0);
        assertThat(dto.getPeerId()).isEqualTo(RECEIVER_ID);
        assertThat(dto.getPeerName()).isEqualTo("Bob");
        assertThat(dto.getPeerEmail()).isEqualTo("bob@test.com");
        assertThat(dto.getContent()).isEqualTo("Hey");
    }

    @Test
    void getInbox_userIsReceiver_peerIsSender() {
        Message msg = makeMessage(1L, SENDER_ID, RECEIVER_ID, "Hi from sender");
        when(messageRepo.findAllUserMessages(RECEIVER_ID)).thenReturn(List.of(msg));
        User peer = makeUser(SENDER_ID, "Alice", "alice@test.com");
        when(userRepo.findById(SENDER_ID)).thenReturn(Optional.of(peer));

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(RECEIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPeerId()).isEqualTo(SENDER_ID);
        assertThat(response.getBody().get(0).getPeerName()).isEqualTo("Alice");
    }

    @Test
    void getInbox_peerNotFoundInRepo_skipsEntry() {
        Message msg = makeMessage(1L, SENDER_ID, RECEIVER_ID, "Hello");
        when(messageRepo.findAllUserMessages(SENDER_ID)).thenReturn(List.of(msg));
        when(userRepo.findById(RECEIVER_ID)).thenReturn(Optional.empty());

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(SENDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getInbox_multipleMsgsFromSamePeer_onlyFirstKept() {
        // Two messages from same peer — inbox should only show the most recent (first in list)
        Message msg1 = makeMessage(1L, SENDER_ID, RECEIVER_ID, "First");
        Message msg2 = makeMessage(2L, SENDER_ID, RECEIVER_ID, "Second");
        when(messageRepo.findAllUserMessages(SENDER_ID)).thenReturn(List.of(msg1, msg2));
        User peer = makeUser(RECEIVER_ID, "Bob", "bob@test.com");
        when(userRepo.findById(RECEIVER_ID)).thenReturn(Optional.of(peer));

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(SENDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getContent()).isEqualTo("First");
    }

    @Test
    void getInbox_multipleDistinctPeers_allIncluded() {
        Long peer2Id = 3L;
        Message msg1 = makeMessage(1L, SENDER_ID, RECEIVER_ID, "To Peer1");
        Message msg2 = makeMessage(2L, SENDER_ID, peer2Id, "To Peer2");
        when(messageRepo.findAllUserMessages(SENDER_ID)).thenReturn(List.of(msg1, msg2));
        when(userRepo.findById(RECEIVER_ID)).thenReturn(Optional.of(makeUser(RECEIVER_ID, "Bob", "b@t.com")));
        when(userRepo.findById(peer2Id)).thenReturn(Optional.of(makeUser(peer2Id, "Carol", "c@t.com")));

        ResponseEntity<List<InboxMessageDto>> response = controller.getInbox(SENDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
}
