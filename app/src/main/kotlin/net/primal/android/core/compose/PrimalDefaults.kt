package net.primal.android.core.compose

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.primal.android.theme.AppTheme


object PrimalDefaults {

    @Composable
    fun outlinedTextFieldColors(
        focusedContainerColor: Color = AppTheme.extraColorScheme.surfaceVariantAlt,
        unfocusedContainerColor: Color = AppTheme.extraColorScheme.surfaceVariantAlt,
        disabledContainerColor: Color = AppTheme.extraColorScheme.surfaceVariantAlt,
        errorBorderColor: Color = AppTheme.colorScheme.error.copy(alpha = 0.5f),
        focusedBorderColor: Color = AppTheme.extraColorScheme.surfaceVariantAlt,
        unfocusedBorderColor: Color = AppTheme.extraColorScheme.surfaceVariantAlt,
    ): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = focusedContainerColor,
        unfocusedContainerColor = unfocusedContainerColor,
        disabledContainerColor = disabledContainerColor,
        errorBorderColor = errorBorderColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
    )

}