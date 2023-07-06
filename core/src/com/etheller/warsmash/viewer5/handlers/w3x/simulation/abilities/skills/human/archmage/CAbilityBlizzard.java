package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.archmage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.units.manager.MutableObjectData.MutableGameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public class CAbilityBlizzard extends CAbilityPointTargetSpellBase {
	private float buildingReduction;
	private float damage;
	private float damagePerSecond;
	private float maximumDamagePerWave;
	private int shardCount;
	private int waveCount;
	private float waveDelay;
	private float areaOfEffect;
	private War3ID effectId;

	private int currentWave;
	private int nextWaveTick;
	private boolean waveForDamage = false;
	private final Rectangle recycleRect = new Rectangle();

	public CAbilityBlizzard(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final MutableGameObject worldEditorAbility, final int level) {
		this.buildingReduction = worldEditorAbility.getFieldAsFloat(AbilityFields.Blizzard.BUILDING_REDUCTION, level);
		this.damage = worldEditorAbility.getFieldAsFloat(AbilityFields.Blizzard.DAMAGE, level);
		this.damagePerSecond = worldEditorAbility.getFieldAsFloat(AbilityFields.Blizzard.DAMAGE_PER_SECOND, level);
		this.maximumDamagePerWave = worldEditorAbility.getFieldAsFloat(AbilityFields.Blizzard.MAXIMUM_DAMAGE_PER_WAVE,
				level);
		this.shardCount = worldEditorAbility.getFieldAsInteger(AbilityFields.Blizzard.NUMBER_OF_SHARDS, level);
		this.waveCount = worldEditorAbility.getFieldAsInteger(AbilityFields.Blizzard.NUMBER_OF_WAVES, level);

		this.waveDelay = getCastingTime();
		setCastingTime(0); // dont use the casting time field normally
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT, level);
		this.effectId = AbstractCAbilityTypeDefinition.getEffectId(worldEditorAbility, level);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.blizzard;
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		this.currentWave = 0;
		this.waveForDamage = false;
		this.nextWaveTick =
				simulation.getGameTurnTick() + (int) StrictMath.ceil(this.waveDelay / WarsmashConstants.SIMULATION_STEP_TIME);
		return true;
	}

	@Override
	public boolean doChannelTick(CSimulation simulation, CUnit caster, AbilityTarget target) {
		if (simulation.getGameTurnTick() >= this.nextWaveTick) {
			final float waveDelay;
			if (this.waveForDamage) {
				this.currentWave++;
				waveDelay = this.waveDelay;
				this.waveForDamage = false;
				List<CUnit> damageTargets = new ArrayList<>();
				simulation.getWorldCollision()
						.enumUnitsInRect(this.recycleRect.set(target.getX() - this.areaOfEffect,
										target.getY() - this.areaOfEffect, this.areaOfEffect * 2, this.areaOfEffect * 2),
								new CUnitEnumFunction() {
									@Override
									public boolean call(final CUnit possibleTarget) {
										if (possibleTarget.canReach(target, CAbilityBlizzard.this.areaOfEffect)
												&& possibleTarget.canBeTargetedBy(simulation, caster,
												getTargetsAllowed())) {
											damageTargets.add(possibleTarget);
										}
										return false;
									}
								});
				float damagePerTarget = this.damage;
				if (damagePerTarget * damageTargets.size() > maximumDamagePerWave) {
					damagePerTarget = maximumDamagePerWave / damageTargets.size();
				}
				float damagePerTargetBuilding = damagePerTarget * (buildingReduction);
				for (CUnit damageTarget : damageTargets) {
					float thisTargetDamage;
					if (damageTarget.isBuilding()) {
						thisTargetDamage = damagePerTargetBuilding;
					}
					else {
						thisTargetDamage = damagePerTarget;
					}
					damageTarget.damage(simulation, caster, CAttackType.SPELLS, CDamageType.COLD,
							CWeaponSoundTypeJass.WHOKNOWS.name(), thisTargetDamage);
				}
			}
			else {
				final Random seededRandom = simulation.getSeededRandom();
				for (int i = 0; i < this.shardCount; i++) {
					final float randomAngle = seededRandom.nextFloat((float) (StrictMath.PI * 2));
					/* (1 - StrictMath.pow(seededRandom.nextFloat(), 2)) */
					final float randomDistance = seededRandom.nextFloat() * this.areaOfEffect;
					simulation.spawnSpellEffectOnPoint(
							target.getX() + ((float) StrictMath.cos(randomAngle) * randomDistance),
							target.getY() + ((float) StrictMath.sin(randomAngle) * randomDistance), 0, this.effectId,
							CEffectType.EFFECT, 0).remove();
				}
				waveDelay = 0.80f;
				this.waveForDamage = true;
			}
			this.nextWaveTick = simulation.getGameTurnTick()
					+ (int) StrictMath.ceil(waveDelay / WarsmashConstants.SIMULATION_STEP_TIME);
		}
		return this.currentWave < this.waveCount;
	}

	@Override
	public float getUIAreaOfEffect() {
		return this.areaOfEffect;
	}

}
