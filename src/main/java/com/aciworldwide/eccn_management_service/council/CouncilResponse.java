package com.aciworldwide.eccn_management_service.council;

import java.util.List;

public record CouncilResponse(
    List<MemberOpinion> stage1Opinions,
    List<PeerReview> stage2Reviews,
    String finalResponse,
    String disclaimer
) {}
