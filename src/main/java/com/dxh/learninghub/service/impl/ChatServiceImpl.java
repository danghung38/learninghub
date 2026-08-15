package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.ChatSendRequest;
import com.dxh.learninghub.dto.response.ChatNotificationResponse;
import com.dxh.learninghub.dto.response.ConversationParticipantResponse;
import com.dxh.learninghub.dto.response.ConversationResponse;
import com.dxh.learninghub.dto.response.MessageResponse;
import com.dxh.learninghub.dto.response.PageResponse;
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
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.ChatService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatServiceImpl implements ChatService {

    ConversationRepository conversationRepository;
    MessageRepository messageRepository;
    UserRepository userRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    CurrentUserProvider currentUserProvider;
    SimpMessagingTemplate messagingTemplate;
    AwsS3Service awsS3Service;

    static final List<EnrollmentStatus> COURSE_CHAT_STATUSES =
            List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    @Transactional
    public ConversationResponse getOrCreateSupportConversation() {
        User currentUser = currentUserProvider.getCurrentUser();

        User admin = userRepository.findByRoles_Name(RoleEnum.ADMIN.name())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        if (Objects.equals(currentUser.getId(), admin.getId())) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);
        }

        Conversation conversation = conversationRepository
                .findExisting(ConversationType.SUPPORT, currentUser, admin, null)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .type(ConversationType.SUPPORT)
                                .initiator(currentUser)
                                .partner(admin)
                                .build()
                ));

        return toConversationResponse(conversation, currentUser.getId(), null, null);
    }

    @Transactional
    public ConversationResponse getOrCreateCourseQaConversation(Long courseId) {
        User currentUser = currentUserProvider.getCurrentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        User teacher = course.getAuthor();

        if (Objects.equals(currentUser.getId(), teacher.getId())) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);
        }

        boolean enrolled = enrollmentRepository.existsByUserAndCourseAndStatusIn(
                currentUser,
                course,
                COURSE_CHAT_STATUSES);
        if (!enrolled) {
            throw new AppException(ErrorCode.COURSE_CHAT_REQUIRES_ENROLLMENT);
        }

        Conversation conversation = conversationRepository
                .findExisting(ConversationType.COURSE_QA, currentUser, teacher, courseId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .type(ConversationType.COURSE_QA)
                                .initiator(currentUser)
                                .partner(teacher)
                                .course(course)
                                .build()
                ));

        return toConversationResponse(conversation, currentUser.getId(), null, null);
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateCourseStudentConversation(Long courseId, Long studentId) {
        User currentUser = currentUserProvider.getCurrentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isCourseOwner = course.getAuthor() != null
                && Objects.equals(course.getAuthor().getId(), currentUser.getId());

        if (!currentUser.isAdmin() && !isCourseOwner) {
            throw new AppException(ErrorCode.NOT_COURSE_OWNER);
        }
        if (Objects.equals(currentUser.getId(), student.getId())) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_YOURSELF);
        }

        boolean enrolled = enrollmentRepository.existsByUserAndCourseAndStatusIn(
                student,
                course,
                COURSE_CHAT_STATUSES);
        if (!enrolled) {
            throw new AppException(ErrorCode.COURSE_CHAT_REQUIRES_ENROLLMENT);
        }

        Conversation conversation = conversationRepository
                .findExisting(ConversationType.COURSE_QA, currentUser, student, courseId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .type(ConversationType.COURSE_QA)
                                .initiator(currentUser)
                                .partner(student)
                                .course(course)
                                .build()
                ));

        return toConversationResponse(conversation, currentUser.getId(), null, null);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations() {
        User currentUser = currentUserProvider.getCurrentUser();

        List<Conversation> conversations = conversationRepository.findAllByParticipant(currentUser);

        if (conversations.isEmpty()) return List.of();

        List<Long> ids = conversations.stream().map(Conversation::getId).toList();

        Map<Long, Message> lastMessageByConversationId = messageRepository
                .findLastMessagesByConversationIds(ids)
                .stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m));

        return conversations.stream()
                .map(c -> {
                    Message last = lastMessageByConversationId.get(c.getId());
                    return toConversationResponse(
                            c,
                            currentUser.getId(),
                            last != null ? last.getContent() : null,
                            last != null ? last.getCreatedAt() : null
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(
            Long conversationId,
            Pageable pageable) {
        User currentUser = currentUserProvider.getCurrentUser();

        Conversation conversation = conversationRepository
                .findByIdAndParticipant(conversationId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND_OR_FORBIDDEN));

        Page<Message> page = messageRepository.findByConversationId(
                conversation.getId(), pageable);
        return PageResponse.<MessageResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(page.stream().map(this::toMessageResponse).toList())
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId) {
        User currentUser = currentUserProvider.getCurrentUser();

        Conversation conversation = conversationRepository
                .findByIdAndParticipantForUpdate(conversationId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND_OR_FORBIDDEN));

        if (Objects.equals(conversation.getInitiator().getId(), currentUser.getId())) {
            conversation.setInitiatorUnreadCount(0);
        } else {
            conversation.setPartnerUnreadCount(0);
        }
    }

    @Override
    @Transactional
    public void sendMessage(String senderUsername, ChatSendRequest request) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Conversation conversation = conversationRepository
                .findByIdAndParticipantForUpdate(request.conversationId(), sender.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND_OR_FORBIDDEN));

        boolean senderIsInitiator = Objects.equals(conversation.getInitiator().getId(), sender.getId());

        User receiver;
        int receiverUnreadCount;

        if (senderIsInitiator) {
            receiver = conversation.getPartner();
            receiverUnreadCount = conversation.getPartnerUnreadCount() + 1;
            conversation.setPartnerUnreadCount(receiverUnreadCount);
        } else {
            receiver = conversation.getInitiator();
            receiverUnreadCount = conversation.getInitiatorUnreadCount() + 1;
            conversation.setInitiatorUnreadCount(receiverUnreadCount);
        }

        conversation.setUpdatedAt(LocalDateTime.now());

        Message message = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender(sender)
                        .type(MessageType.valueOf(request.type()))
                        .content(request.content().trim())
                        .build()
        );

        MessageResponse messageResponse = toMessageResponse(message);

        ChatNotificationResponse notificationResponse =
                ChatNotificationResponse.builder()
                        .conversationId(conversation.getId())
                        .message(messageResponse)
                        .unreadCount(receiverUnreadCount)
                        .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversation.getId(),
                messageResponse
        );

        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/chat-notifications",
                notificationResponse
        );
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            Long currentUserId,
            String lastMessage,
            LocalDateTime lastMessageAt
    ) {
        boolean currentUserIsInitiator = Objects.equals(conversation.getInitiator().getId(), currentUserId);

        User otherParticipant;
        int unreadCount;

        if (currentUserIsInitiator) {
            otherParticipant = conversation.getPartner();
            unreadCount = conversation.getInitiatorUnreadCount();
        } else {
            otherParticipant = conversation.getInitiator();
            unreadCount = conversation.getPartnerUnreadCount();
        }

        return ConversationResponse.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType().name())
                .otherParticipant(
                        ConversationParticipantResponse.builder()
                                .id(otherParticipant.getId())
                                .fullName(otherParticipant.getFullName())
                                .avatar(awsS3Service.resolveFileUrl(otherParticipant.getAvatar()))
                                .build()
                )
                .courseId(conversation.getCourse() != null ? conversation.getCourse().getId() : null)
                .courseTitle(conversation.getCourse() != null ? conversation.getCourse().getTitle() : null)
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                .build();
    }

    private MessageResponse toMessageResponse(Message m) {
        String fileUrl = m.getContent();

        if (m.getType() == MessageType.IMAGE) {
            fileUrl = awsS3Service.resolveFileUrl(fileUrl);
        }

        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .type(m.getType())
                .content(fileUrl)
                .createdAt(m.getCreatedAt())
                .build();

    }


}
