package com.finance.dto;

import java.util.List;

import com.finance.entity.SurveySuggest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SurveySuggestRequest {

    private Integer surveyId;
    private List<SurveySuggest> suggestions;

}
