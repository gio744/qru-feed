package br.com.qru.transito.domain.usecase

import java.time.LocalDate

enum class SuspensionState(val label: String) {
    UNKNOWN("Não conferida"), ONGOING("Período em curso"), COMPLETED("Período encerrado"), NONE("Sem suspensão registrada")
}
enum class RecyclingState(val label: String) {
    UNKNOWN("Não conferida"), PENDING("Curso ou exame pendente"), APPROVED("Aprovação registrada"), NOT_REQUIRED("Sem exigência identificada")
}
enum class ExpiryState(val label: String) {
    UNKNOWN("Não conferida"), CURRENT("Dentro da validade"), OVER_THIRTY("Vencida há mais de 30 dias")
}
data class GuideFacts(
    val date: String = "",
    val suspension: SuspensionState = SuspensionState.UNKNOWN,
    val recycling: RecyclingState = RecyclingState.UNKNOWN,
    val expiry: ExpiryState = ExpiryState.UNKNOWN
)
data class GuideReview(val checks: List<String>, val referenceCodes: List<String>, val dateValid: Boolean)

object HabilitationGuide {
    fun review(facts: GuideFacts): GuideReview {
        val date = try { LocalDate.parse(facts.date) } catch (_: Exception) { null }
        val checks = mutableListOf<String>()
        val codes = linkedSetOf<String>()
        if (date == null) checks += "Informe uma data válida para conferir a norma aplicável ao fato."
        else checks += "Data do fato: ${date}. Confira a vigência dos atos antes de concluir a análise."
        when (facts.suspension) {
            SuspensionState.UNKNOWN -> checks += "Confira no Renach as datas de início e término da suspensão."
            SuspensionState.ONGOING -> {
                checks += "Período informado como em curso: confronte as datas e o registro com a data do fato."
                codes += "502-92"
            }
            SuspensionState.COMPLETED -> {
                checks += "Período informado como encerrado: confira separadamente curso e aprovação no exame."
                if (facts.recycling == RecyclingState.PENDING) codes += "774-92"
            }
            SuspensionState.NONE -> checks += "Ausência de suspensão informada: confira se há outra condição registrada."
        }
        when (facts.recycling) {
            RecyclingState.UNKNOWN -> checks += "Confira se há exigência de reciclagem e o resultado do exame no Renach."
            RecyclingState.PENDING -> {
                checks += "Individualize a pendência: realização do curso, exame teórico ou registro da aprovação."
                codes += "774-92"
            }
            RecyclingState.APPROVED -> checks += "Confira a aprovação no Renach; certificado de curso e aprovação são etapas distintas."
            RecyclingState.NOT_REQUIRED -> checks += "Confirme a ausência de exigência; curso preventivo não se confunde com reciclagem obrigatória."
        }
        when (facts.expiry) {
            ExpiryState.UNKNOWN -> checks += "Confira o vencimento do documento e eventuais prorrogações."
            ExpiryState.CURRENT -> checks += "Validade informada: confira o documento e o registro na data do fato."
            ExpiryState.OVER_THIRTY -> {
                checks += "Confira a data original de vencimento e a possível prorrogação antes de analisar a referência de CNH vencida."
                codes += "504-50"
                if (date != null && date.year == 2026) checks += "Para fatos de 2026, confira o alcance e os prazos da Deliberação 279/2026."
            }
        }
        checks += "Registre a condução constatada, os dados consultados e a condição do proprietário para analisar responsabilidade."
        checks += "Referências relacionadas para conferência; esta organização não conclui infração nem autoriza autuação."
        return GuideReview(checks, codes.toList(), date != null)
    }
}
