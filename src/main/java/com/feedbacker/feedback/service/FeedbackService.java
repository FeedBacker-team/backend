package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.ChoiceQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.domain.dto.request.SubjectiveQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.response.*;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.AcornHistoryRepository;
import com.feedbacker.feedback.service.mapper.QuestionAnswerResponseMapper;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.feedbackpost.repository.QuestionRepository;
import com.feedbacker.feedbackpost.service.ParticipationService;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.global.image.ImageRequest;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final ParticipationService participationService;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackPostRepository feedbackPostRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final MemberRepository memberRepository;
    private final AcornHistoryRepository acornHistoryRepository;
    private final ParticipationRepository participationRepository;
    private final QuestionAnswerResponseMapper questionAnswerResponseMapper;

    @Transactional
    public UUID submit(FeedbackSubmitRequest request) {
        UUID testerId = UUID.randomUUID(); // 멤버 아이디 찾기
        Member tester = getMember(testerId);

        FeedbackPost feedbackPost = getFeedbackPost(request.feedbackPostId());
        feedbackPost.validateRecruiting();

        participationService.validateAccessAuth(testerId);

        List<QuestionAnswer> answers = createAnswers(request.choiceAnswers(), request.subjectiveAnswers());

        Feedback savedFeedback = feedbackRepository.save(
                Feedback.create(feedbackPost, tester, answers)
        );

        return savedFeedback.getId();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMine() {
        UUID memberId = UUID.randomUUID();
        List<Feedback> feedbacks = feedbackRepository.getAllByTesterId(memberId);
        return FeedbackResponse.fromAll(feedbacks);
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getDetail(UUID feedbackId) {
        
        Feedback feedback = getFeedback(feedbackId);
        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());
        Member member = getMember(feedback.getTesterId());
        Participation participation = participationRepository.findByTester(member);

        List<Question> questions = questionRepository.findAllByFeedbackPostIdOrderByOrderAsc(feedback.getFeedbackPostId());
        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(feedbackId);
        QuestionAnswerResponse questionAnswerResponse = questionAnswerResponseMapper.toResponse(questions, questionAnswers);

        return FeedbackDetailResponse.from(
                feedbackPost,
                feedback,
                participation,
                questionAnswerResponse
        );
    }

    @Transactional
    public void accept(UUID feedbackId) {
        Feedback feedback = getFeedback(feedbackId);
        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());

        UUID testerId = UUID.randomUUID();
        UUID writerId = UUID.randomUUID();
        Member tester = getMember(testerId);
        Member writer = getMember(writerId);
        validateAccessAuth(feedbackPost, testerId);

        feedback.setRewardAcorn(feedbackPost.getRewardAcorn());
        feedback.setStatus(FeedbackStatus.ACCEPTED);

        acornHistoryRepository.saveAll(
                AcornHistory.create(
                        tester,
                        writer,
                        feedback,
                        feedbackPost
                )
        );
    }

    @Transactional
    public void reject(UUID feedbackId) {
        Feedback feedback = getFeedback(feedbackId);
        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());
        UUID memberId = UUID.randomUUID();
        validateAccessAuth(feedbackPost, memberId);
        feedback.setStatus(FeedbackStatus.REJECTED);
        // 이후 거부 타입 & 거부 상세 이유 추가 예정
    }

    @Transactional(readOnly = true)
    public FeedbackResultResponse getResult(UUID feedbackId) {
        Feedback feedback = getFeedback(feedbackId);
        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());

        UUID memberId = UUID.randomUUID();
        Member member = getMember(memberId);

        AcornHistoryResponse acornHistoryResponse = AcornHistoryResponse.from(
                acornHistoryRepository.findByMemberAndFeedbackId(member, feedbackId)
        );

        return FeedbackResultResponse.from(feedback, feedbackPost, acornHistoryResponse);
    }

    @Transactional
    public void object(UUID feedbackId, String objectReason) {
        Feedback feedback = getFeedback(feedbackId);
        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());
        UUID memberId = UUID.randomUUID();
        validateAccessAuth(feedbackPost, memberId);
        feedback.setObjectReason(objectReason);
        // 이후 어드민에 이의제기 신청 알림 추가할 예정
    }

    private List<QuestionAnswer> createAnswers(
            List<ChoiceQuestionAnswerRequest> choiceAnswers,
            List<SubjectiveQuestionAnswerRequest> subjectiveAnswers
    ) {

        List<Long> questionIds = Stream.concat(
                choiceAnswers.stream().map(ChoiceQuestionAnswerRequest::order),
                subjectiveAnswers.stream().map(SubjectiveQuestionAnswerRequest::order)
        ).toList();

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        List<QuestionAnswer> answers = new ArrayList<>();

        for (ChoiceQuestionAnswerRequest answer : choiceAnswers) {
            Question question = questionMap.get(answer.order());
            if (question == null) {
                throw new RuntimeException();
            }

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .images(answer.images().stream()
                            .map(ImageRequest::toImageInfo)
                            .toList())
                    .selectedOption(answer.selectedOption())
                    .build();

            answers.add(questionAnswer);
        }

        for (SubjectiveQuestionAnswerRequest answer : subjectiveAnswers) {
            Question question = questionMap.get(answer.order());
            if (question == null) {
                throw new RuntimeException();
            }

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .subjectiveAnswer(answer.text())
                    .build();

            answers.add(questionAnswer);
        }

        return questionAnswerRepository.saveAll(answers);
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(RuntimeException::new);
    }

    private Feedback getFeedback(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackErrorCode.FEEDBACK_NOT_FOUND
                ));
    }

    private FeedbackPost getFeedbackPost(UUID feedbackPostId) {
        return feedbackPostRepository.findById(feedbackPostId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND
                ));
    }

    private void validateAccessAuth(FeedbackPost feedbackPost,UUID memberId) {
        if (memberId != feedbackPost.getWriterId()) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

}
