package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.ConversationRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.repo.MessageRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    ConversationRepository conversationRepository;

    @Mock
    MessageRepository messageRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CourseRepository courseRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    AwsS3Service awsS3Service;

    @InjectMocks
    ChatServiceImpl chatService;

    @Test
    void getOrCreateSupportConversation_rejectsAdminChattingWithSelf() {
        User admin = User.builder()
                .username("admin")
                .fullName("Admin LearningHub")
                .email("admin@learninghub.test")
                .build();
        admin.setId(1L);

        when(currentUserProvider.getCurrentUser()).thenReturn(admin);
        when(userRepository.findByRoles_Name(RoleEnum.ADMIN.name()))
                .thenReturn(List.of(admin));

        assertThatThrownBy(chatService::getOrCreateSupportConversation)
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);

        verify(conversationRepository, never())
                .findExisting(any(), any(), any(), any());
        verify(conversationRepository, never()).save(any());
    }
}
