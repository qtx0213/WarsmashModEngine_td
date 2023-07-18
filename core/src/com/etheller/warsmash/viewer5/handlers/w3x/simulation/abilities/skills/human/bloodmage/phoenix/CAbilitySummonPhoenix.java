package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage.phoenix;

import com.etheller.warsmash.units.manager.MutableObjectData.MutableGameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

import java.util.ArrayList;
import java.util.List;

public class CAbilitySummonPhoenix extends CAbilityNoTargetSpellBase {
	private War3ID summonUnitId;
	private int summonUnitCount;
	private War3ID buffId;
	private float areaOfEffect;

	// TODO maybe "lastSummonHandleIds" instead, for ease of use with saving game,
	// but then we have to track when they die or else risk re-used handle ID
	// messing us up
	private final List<CUnit> lastSummonUnits = new ArrayList<>();

	public CAbilitySummonPhoenix(int handleId, War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(MutableGameObject worldEditorAbility, int level) {
		this.summonUnitId = War3ID
				.fromString(worldEditorAbility.getFieldAsString(AbilityFields.SummonWaterElemental.SUMMONED_UNIT_TYPE, level));
		this.summonUnitCount = worldEditorAbility.getFieldAsInteger(AbilityFields.SummonWaterElemental.SUMMONED_UNIT_COUNT, level);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT, level);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.summonphoenix;
	}

	@Override
	public boolean doEffect(CSimulation simulation, CUnit unit, AbilityTarget target) {
		float facing = unit.getFacing();
		float facingRad = (float) StrictMath.toRadians(facing);
		float x = unit.getX() + ((float) StrictMath.cos(facingRad) * areaOfEffect);
		float y = unit.getY() + ((float) StrictMath.sin(facingRad) * areaOfEffect);
		for (final CUnit lastSummon : this.lastSummonUnits) {
			if (!lastSummon.isDead()) {
				lastSummon.kill(simulation);
			}
		}
		this.lastSummonUnits.clear();
		for (int i = 0; i < summonUnitCount; i++) {
			CUnit summonedUnit = simulation.createUnitSimple(summonUnitId, unit.getPlayerIndex(), x, y, facing);
			summonedUnit.addClassification(CUnitClassification.SUMMONED);
			summonedUnit.add(simulation,
					new CBuffTimedLife(simulation.getHandleIdAllocator().createId(), buffId, getDuration(), false));
			simulation.createSpellEffectOnUnit(summonedUnit, getAlias(), CEffectType.TARGET);
			this.lastSummonUnits.add(summonedUnit);
		}
		return false;
	}

	public War3ID getSummonUnitId() {
		return summonUnitId;
	}

	public int getSummonUnitCount() {
		return summonUnitCount;
	}

	public War3ID getBuffId() {
		return buffId;
	}

	public float getAreaOfEffect() {
		return areaOfEffect;
	}

	public void setSummonUnitId(War3ID summonUnitId) {
		this.summonUnitId = summonUnitId;
	}

	public void setSummonUnitCount(int summonUnitCount) {
		this.summonUnitCount = summonUnitCount;
	}

	public void setBuffId(War3ID buffId) {
		this.buffId = buffId;
	}

	public void setAreaOfEffect(float areaOfEffect) {
		this.areaOfEffect = areaOfEffect;
	}

}
