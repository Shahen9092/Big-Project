package org.example.routes

import org.example.pages.TemplateExerciseItem
import org.example.pages.TemplateSummary

data class TemplatesPageData(
    val templates: List<TemplateSummary>,
    val exercises: List<TemplateExerciseItem>
)

data class TemplateDetailPageData(
    val template: TemplateSummary,
    val exercises: List<TemplateExerciseItem>
)