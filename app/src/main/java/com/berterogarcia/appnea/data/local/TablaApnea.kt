package com.berterogarcia.appnea.data.local

data class TablaApnea(
    val nombre: String,
    val fases: List<Fase>
)

object TablasPredefinidas {

    fun tabata(
        apneaSeg: Int = 90,
        descansoSeg: Int = 60,
        repeticiones: Int = 4
    ): TablaApnea {
        val fases = buildList {
            add(Fase(TipoFase.PREPARACION, 60))
            repeat(repeticiones) { i ->
                add(Fase(TipoFase.APNEA, apneaSeg))
                if (i < repeticiones - 1) add(Fase(TipoFase.DESCANSO, descansoSeg))
            }
        }
        return TablaApnea(
            "Tabata", // el detalle (4 × 1:30 apnea / 1:00 descanso) lo arma la tarjeta
            fases
        )
    }

    fun piramide(
        base: Int = 30, incremento: Int = 15,
        escalones: Int = 5, descansoSeg: Int = 90
    ): TablaApnea {
        val fases = buildList {
            add(Fase(TipoFase.PREPARACION, 60))
            for (i in 0 until escalones) {
                add(Fase(TipoFase.APNEA, base + incremento * i))
                if (i < escalones - 1) add(Fase(TipoFase.DESCANSO, descansoSeg))
            }
        }
        return TablaApnea("Pirámide", fases)
    }

    // Ejemplo de Franco: 3' preparación, 2' apnea, 1' descanso, 2' apnea, 1' descanso, 3' apnea
    fun progresiva321213(): TablaApnea = TablaApnea(
        "Progresiva 3-2-1-2-1-3",
        listOf(
            Fase(TipoFase.PREPARACION, 180),
            Fase(TipoFase.APNEA, 120),
            Fase(TipoFase.DESCANSO, 60),
            Fase(TipoFase.APNEA, 120),
            Fase(TipoFase.DESCANSO, 60),
            Fase(TipoFase.APNEA, 180)
        )
    )

    fun listaBase() = listOf(tabata(), piramide(), progresiva321213())
}
