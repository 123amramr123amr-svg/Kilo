package com.amrstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmrStudioTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    colors: androidx.compose.material3.TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    )
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title, fontWeight = FontWeight.Medium, fontSize = 20.sp) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors
    )
}

@Composable
func AmrStudioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun AmrStudioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(text = text, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}

@Composable
fun AmrStudioOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            borderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(text = text, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}

@Composable
func AmrStudioTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String = "",
    keyboardOptions: androidx.compose.ui.text.input.KeyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default,
    keyboardActions: androidx.compose.ui.text.input.KeyboardActions = androidx.compose.ui.text.input.KeyboardActions.Default,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        isError = error != null,
        supportingText = { if (error != null) Text(text = error, color = androidx.compose.material3.MaterialTheme.colorScheme.error) },
        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

@Composable
func AmrStudioIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        icon()
    }
}

@Composable
func AmrStudioChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(text = text) },
        leadingIcon = icon,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            selectedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            unselectedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
func AmrStudioAvatar(
    name: String,
    modifier: Modifier = Modifier.size(40.dp),
    backgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.ui.graphics.shape.CircleShape)
            .background(backgroundColor),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
func AmrStudioDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.Divider(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
}

@Composable
func AmrStudioEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    actionText: String? = null,
    action: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        icon()
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(text = message, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        actionText?.let { text ->
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            AmrStudioButton(text = text, onClick = action!!)
        }
    }
}