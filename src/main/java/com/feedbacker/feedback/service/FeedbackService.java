package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.*;
import com.feedbacker.feedback.domain.dto.request.ChoiceQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.domain.dto.request.SubjectiveQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResultResponse;
import com.feedbacker.feedback.service.mapper.FeedbackDetailMapper;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.repository.QuestionRepository;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FeedbackService {

//    private final FeedbackPostRepository feedbackPostRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final MemberRepository memberRepository;
    private final FeedbackDetailMapper feedbackDetailMapper;

    @Transactional
    public UUID submit(FeedbackSubmitRequest request) {
        UUID memberId = UUID.randomUUID(); // 멤버 아이디 찾기
        Member tester = memberRepository.findById(memberId)
                .orElseThrow(RuntimeException::new);

//        FeedbackPost feedbackPost = feedbackPostRepository.findById(feedback.getFeedbackPostId())
//                .orElseThrow(RuntimeException::new);
        FeedbackPost feedbackPost = null;

        List<QuestionAnswer> answers = createAnswers(request.choiceAnswers(), request.subjectiveAnswers());

        Feedback feedback = Feedback.builder()
                .feedbackPostId(request.feedbackPostId())
                .testerId(memberId)
                .testerName(tester.getNickname())
                .postTitle(feedbackPost.getTitle())
                .status(FeedbackStatus.SUBMITTED)
                .answers(answers)
                .submitAt(LocalDateTime.now())
                .build();

        feedbackRepository.save(feedback);

        return feedback.getId();
    }

    public List<FeedbackResponse> getAll() {
        UUID memberId = UUID.randomUUID();
        List<Feedback> feedbacks = feedbackRepository.getAllByTesterId(memberId);

        return FeedbackResponse.fromAll(feedbacks);
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getDetail(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RuntimeException::new);

        List<Question> questions = questionRepository.findAllByFeedbackPostIdOrderByOrderAsc(feedback.getFeedbackPostId());
        List<QuestionAnswer> questionAnswers = questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(feedbackId);

        return feedbackDetailMapper.toResponse(questions, questionAnswers);
    }

    @Transactional
    public void accept(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RuntimeException::new);

//        FeedbackPost feedbackPost = feedbackPostRepository.findById(feedback.getFeedbackPostId())
//                .orElseThrow(RuntimeException::new);
        FeedbackPost feedbackPost = null;

        feedback.setRewardAcorn(feedbackPost.getRewardAcorn());
        feedback.setStatus(FeedbackStatus.ACCEPTED);
        // 도토리 거래 내역 저장 추가할 예정
    }

    @Transactional
    public void reject(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RuntimeException::new);

        feedback.setStatus(FeedbackStatus.REJECTED);
        // 이후 거부 타입 & 거부 상세 이유 추가 예정
    }

    public FeedbackResultResponse getResult(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RuntimeException::new);

//        FeedbackPost feedbackPost = feedbackPostRepository.findById(feedback.getFeedbackPostId())
//                .orElseThrow(RuntimeException::new);
        FeedbackPost feedbackPost = null;

        // 유저 정보 or 거래 내역에서 도토리 정보 찾을 예정
        AcornChange acornChange = null;

        return FeedbackResultResponse.from(feedback, feedbackPost, acornChange);
    }

    @Transactional
    public void object(UUID feedbackId, String objectReason) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RuntimeException::new);

        feedback.setObjectReason(objectReason);
        // 이후 어드민에 이의제기 신청 알림 추가할 예정
    }

    private List<QuestionAnswer> createAnswers(
            List<ChoiceQuestionAnswerRequest> choiceAnswers,
            List<SubjectiveQuestionAnswerRequest> subjectiveAnswers
    ) {

        List<Long> questionIds = Stream.concat(
                choiceAnswers.stream().map(ChoiceQuestionAnswerRequest::questionId),
                subjectiveAnswers.stream().map(SubjectiveQuestionAnswerRequest::questionId)
        ).toList();

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        List<QuestionAnswer> answers = new ArrayList<>();

        for (ChoiceQuestionAnswerRequest answer : choiceAnswers) {
            Question question = questionMap.get(answer.questionId());
            if (question == null) {
                throw new RuntimeException();
            }

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .choiceOption(answer.selectedOption())
                    .build();

            answers.add(questionAnswer);
        }

        for (SubjectiveQuestionAnswerRequest answer : subjectiveAnswers) {
            Question question = questionMap.get(answer.questionId());
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

}
