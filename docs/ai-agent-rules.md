# AI Agent Rules for ECCN Classification System

## Core Classification Rules

1. **Deterministic Output**
   - Always provide consistent ECCN classifications for identical inputs
   - Log and explain any classification variations due to context changes

2. **Regulatory Compliance**
   - Stay updated with latest EAR regulations
   - Default to stricter classification when uncertain
   - Never suggest classifications without proper analysis

3. **Risk Assessment**
   - Flag high-risk patterns in source code analysis
   - Identify potential dual-use capabilities
   - Monitor for encryption strength thresholds

## Operational Boundaries

1. **Classification Scope**
   - Only classify within trained ECCN categories
   - Defer to human review for novel or complex cases
   - Maintain clear audit trails of classification decisions

2. **Data Handling**
   - Never store sensitive source code
   - Process only necessary code segments for classification
   - Maintain confidentiality of analysis results

3. **Integration Rules**
   - Respect service boundaries defined in bounded contexts
   - Use standardized interfaces for inter-service communication
   - Follow established event patterns for updates

## Quality Controls

1. **Validation**
   - Cross-validate classifications with multiple models
   - Maintain confidence scores for suggestions
   - Flag classifications requiring human review

2. **Learning Boundaries**
   - Only learn from validated classification decisions
   - Maintain version control of learning models
   - Document all training data sources

3. **Error Handling**
   - Fail gracefully with clear error messages
   - Never skip validation steps
   - Maintain full audit trail of errors

## Security Requirements

1. **Access Control**
   - Verify authentication for all requests
   - Respect role-based access controls
   - Log all classification attempts

2. **Data Protection**
   - Encrypt all stored classification data
   - Implement secure communication channels
   - Follow data retention policies