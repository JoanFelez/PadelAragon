package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup

class SortGroupsUseCase {
    operator fun invoke(groups: List<LeagueGroup>): List<LeagueGroup> {
        return groups.sortedWith(
            compareBy<LeagueGroup> {
                when (it.gender) {
                    Gender.MASCULINA -> 0
                    Gender.FEMENINA -> 1
                }
            }.thenBy { it.category }
                .thenBy { it.name }
        )
    }
}
