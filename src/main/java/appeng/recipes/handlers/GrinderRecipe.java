/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.recipes.handlers;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import appeng.core.AppEng;

public class GrinderRecipe implements Recipe<Container> {

    public static final ResourceLocation TYPE_ID = AppEng.makeId("grinder");

    public static final RecipeType<GrinderRecipe> TYPE = RecipeType.register(TYPE_ID.toString());

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemStack result;
    private final List<GrinderOptionalResult> optionalResults;
    private final int turns;

    public GrinderRecipe(ResourceLocation id, Ingredient ingredient, int ingredientCount,
            ItemStack result, int turns, List<GrinderOptionalResult> optionalResults) {
        this.id = id;
        this.ingredient = ingredient;
        this.ingredientCount = ingredientCount;
        this.result = result;
        this.turns = turns;
        this.optionalResults = ImmutableList.copyOf(optionalResults);
    }

    @Override
    public boolean matches(Container inv, Level level) {
        return this.ingredient.test(inv.getItem(0));
    }

    @Override
    public ItemStack assemble(Container inv) {
        // FIXME: What about secondary output
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GrinderRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getTurns() {
        return turns;
    }

    public int getIngredientCount() {
        return ingredientCount;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();
        nonnulllist.add(this.ingredient);
        return nonnulllist;
    }

    public List<GrinderOptionalResult> getOptionalResults() {
        return optionalResults;
    }

}
