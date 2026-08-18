package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.request.ChatSendRequest;
import com.dxh.learninghub.entity.Conversation;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Message;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.ConversationType;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.enums.MessageType;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.ConversationRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.MessageRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.ChatServiceImpl;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock AwsS3Service awsS3Service;
    @InjectMocks ChatServiceImpl service;

    @Test
    void getOrCreateSupportConversation_requiresAdmin() {
        when(currentUserProvider.getCurrentUser()).thenReturn(user(3L, "student"));
        when(userRepository.findByRoles_Name(RoleEnum.ADMIN.name())).thenReturn(List.of());

        assertError(service::getOrCreateSupportConversation, ErrorCode.ADMIN_NOT_FOUND);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void getOrCreateCourseQaConversation_requiresEnrollment() {
        User student = user(3L, "student");
        User teacher = user(4L, "teacher");
        Course course = Course.builder().author(teacher).title("Java").build();
        course.setId(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserAndCourseAndStatusIn(
                eq(student), eq(course), anyList())).thenReturn(false);

        assertError(() -> service.getOrCreateCourseQaConversation(7L),
                ErrorCode.COURSE_CHAT_REQUIRES_ENROLLMENT);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void sendMessage_persistsTrimmedTextAndNotifiesReceiver() {
        User sender = user(3L, "student");
        User receiver = user(4L, "teacher");
        Conversation conversation = Conversation.builder().type(ConversationType.SUPPORT)
                .initiator(sender).partner(receiver).partnerUnreadCount(2).build();
        conversation.setId(9L);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(sender));
        when(conversationRepository.findByIdAndParticipantForUpdate(9L, 3L))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(12L);
            return message;
        });

        service.sendMessage("student", ChatSendRequest.builder()
                .conversationId(9L).type(MessageType.TEXT.name()).content("  xin chào  ").build());

        assertThat(conversation.getPartnerUnreadCount()).isEqualTo(3);
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(message.capture());
        assertThat(message.getValue().getContent()).isEqualTo("xin chào");
        assertThat(message.getValue().getType()).isEqualTo(MessageType.TEXT);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/9"), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(eq("teacher"), eq("/queue/chat-notifications"), any(Object.class));
    }

    @Test
    void markAsRead_resetsOnlyCurrentParticipantCounter() {
        User initiator = user(3L, "student");
        User partner = user(4L, "teacher");
        Conversation conversation = Conversation.builder().initiator(initiator).partner(partner)
                .initiatorUnreadCount(5).partnerUnreadCount(7).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(initiator);
        when(conversationRepository.findByIdAndParticipantForUpdate(9L, 3L))
                .thenReturn(Optional.of(conversation));

        service.markAsRead(9L);

        assertThat(conversation.getInitiatorUnreadCount()).isZero();
        assertThat(conversation.getPartnerUnreadCount()).isEqualTo(7);
    }

    private static User user(Long id, String username) {
        User user = User.builder().username(username).fullName(username).build();
        user.setId(id);
        return user;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
