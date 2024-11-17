package com.brother.big.integration.llm_utils

object LLMPromptConsts {
    const val MOTIVATION_BASICS = "You are a strong code line analyzer"

    const val JSON_EXPECTATION = "DO NOT WRITE ANYTHING EXCEPT JSON OUTPUT"

    const val ANALYSE_REQUEST = """
            The score for each field should be a number from 1 to 10. 
            It is necessary that you select as many competencies as possible from the lines of code and fill out the appropriate competency assessments for them.
            If there is no data at all for any competence, specify the score as 0.
            The assessment should primarily depend on the complexity, accuracy of the written code and depth of knowledge, and only then on the number of lines corresponding to the competence.
            The score cannot be high without demonstrating a deep level of knowledge.
            You also need to give a detailed brief comment on the entire competence matrix in 'summary' field.
            In the 'summary', briefly summarize developer skills(in 1-3 sentences), and also add to 'summary' what grade you would give the developer based on his competencies.
    """

    const val MERGE_REQUEST = """
            You need to make one resulting json matrix out of these json matrix schemas according to answer JSON Schema. This final scheme should be a 
            combination of developer skills based on the schemes given to you. The scheme must have the same structure 
            as the input scheme. For numerical parameters, the final scheme should have the largest value of all the 
            values of these parameters in the input schemes, but not over than 10. For the 'summary' parameter, you need to collect short summary of 
            all incoming summaries, which will be based on the analysis of incoming summaries and include all descriptions
            of the skills mentioned in the incoming summaries from the 'summary' parameter of json schemes. There should be no 
            repetitions in the 'summary' text, but there should be detailed information about each skill, you have to write summary in 1-8 sentences.
            Make such a resulting matrix.  
    """
}