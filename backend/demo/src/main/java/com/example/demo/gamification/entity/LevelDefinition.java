package com.example.demo.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "level_definition")
public class LevelDefinition {

    @Id
    private Integer level;

    @Column(name = "rank_name", nullable = false, length = 100)
    private String rankName;

    @Column(name = "exp_required_to_reach", nullable = false)
    private int expRequiredToReach;

    @Column(name = "shield_max_total", nullable = false)
    private int shieldMaxTotal;

    @Column(name = "unlock_description", nullable = false, length = 500)
    private String unlockDescription;

    protected LevelDefinition() {
    }

    public LevelDefinition(Integer level, String rankName, int expRequiredToReach, int shieldMaxTotal, String unlockDescription) {
        this.level = level;
        this.rankName = rankName;
        this.expRequiredToReach = expRequiredToReach;
        this.shieldMaxTotal = shieldMaxTotal;
        this.unlockDescription = unlockDescription;
    }

    public Integer getLevel() {
        return level;
    }

    public String getRankName() {
        return rankName;
    }

    public int getExpRequiredToReach() {
        return expRequiredToReach;
    }

    public int getShieldMaxTotal() {
        return shieldMaxTotal;
    }

    public String getUnlockDescription() {
        return unlockDescription;
    }
}
