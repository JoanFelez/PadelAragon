package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.LeagueGroup

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
