package com.amrstudio.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amrstudio.R
import com.amrstudio.ui.components.AmrStudioButton
import com.amrstudio.ui.components.AmrStudioOutlinedButton
import com.amrstudio.ui.components.AmrStudioTextField
import com.amrstudio.ui.theme.AmrStudioTheme
import com.amrstudio.ui.theme.ColorSchemeType
import com.amrstudio.utils.ValidationUtils
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSuccess: (String) -> Unit,
    onShowDeveloper: () -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val loginResult by viewModel.loginState.collectAsState()
    val registerResult by viewModel.registerState.collectAsState()

    val showRegister = remember { mutableStateOf(false) }
    val showPrivacy = remember { mutableStateOf(false) }
    val showTerms = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Handle results
    androidx.compose.runtime.LaunchedEffect(loginResult) {
        loginResult?.let {
            when (it) {
                is AuthViewModel.AuthResult.Success -> onLoginSuccess(it.user.id)
                is AuthViewModel.AuthResult.Error -> { /* Show error toast */ }
            }
            viewModel.clearResults()
        }
    }

    androidx.compose.runtime.LaunchedEffect(registerResult) {
        registerResult?.let {
            when (it) {
                is AuthViewModel.AuthResult.Success -> onLoginSuccess(it.user.id)
                is AuthViewModel.AuthResult.Error -> { /* Show error toast */ }
            }
            viewModel.clearResults()
        }
    }

    // Privacy/Terms dialogs
    if (showPrivacy.value) {
        AlertDialog(
            onDismissRequest = { showPrivacy.value = false },
            title = { Text("سياسة الخصوصية", fontWeight = FontWeight.Bold) },
            text = { Text("بياناتك محفوظة محلياً على جهازك فقط. لا نرسل أي بيانات لخوادم خارجية إلا بموافقتك الصريحة عند استخدام خدمات AI السحابية.") },
            confirmButton = { TextButton(onClick = { showPrivacy.value = false }) { Text("موافق") } }
        )
    }

    if (showTerms.value) {
        AlertDialog(
            onDismissRequest = { showTerms.value = false },
            title = { Text("شروط الاستخدام", fontWeight = FontWeight.Bold) },
            text = { Text("باستخدامك للتطبيق، أنت توافق على عدم إساءة استخدام خدمات AI، وعدم محاولة الوصول غير المصرح به، واحترام حقوق الملكية الفكرية.") },
            confirmButton = { TextButton(onClick = { showTerms.value = false }) { Text("موافق") } }
        )
    }

    AmrStudioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo/Brand
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Amr Studio",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

                Text(
                    text = "مساعدون عمرو",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Text(
                    text = "منصة الذكاء الاصطناعي للمحادثة والبرمجة والمشاريع",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Text(
                    text = "Amr Studio",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))

                // Demo notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠� نسخة تجريبية محلية - البيانات محفوظة على جهازك فقط",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

                // Form
                if (showRegister.value) {
                    RegisterForm(viewModel, uiState, showPrivacy, showTerms)
                } else {
                    LoginForm(viewModel, uiState)
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

                // Toggle between login/register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (showRegister.value) "لديك حساب بالفعل؟ " else "لا تملك حساب؟ ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { showRegister.value = !showRegister.value },
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (showRegister.value) "تسجيل دخول" : "إنشاء حساب",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

                // Developer access
                AmrStudioOutlinedButton(
                    text = "لوحة المطور",
                    onClick = onShowDeveloper,
                    icon = { Icon(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "", tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

@Composable
fun LoginForm(
    viewModel: AuthViewModel,
    uiState: AuthViewModel.AuthUiState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AmrStudioTextField(
            label = "البريد الإلكتروني",
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            error = uiState.emailError,
            placeholder = "example@domain.com",
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        AmrStudioTextField(
            label = "كلمة المرور",
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            error = uiState.passwordError,
            placeholder = "••••••••",
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = { /* Toggle visibility */ }
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        AmrStudioButton(
            text = if (uiState.isLoading) "جاري الدخول..." else "دخول",
            onClick = viewModel::validateAndLogin,
            enabled = !uiState.isLoading
        )
    }
}

@Composable
fun RegisterForm(
    viewModel: AuthViewModel,
    uiState: AuthViewModel.AuthUiState,
    showPrivacy: androidx.compose.runtime.MutableState<Boolean>,
    showTerms: androidx.compose.runtime.MutableState<Boolean>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AmrStudioTextField(
            label = "الاسم",
            value = uiState.name,
            onValueChange = viewModel::updateName,
            error = uiState.nameError,
            placeholder = "أحمد محمد"
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        AmrStudioTextField(
            label = "البريد الإلكتروني",
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            error = uiState.emailError,
            placeholder = "example@domain.com",
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        AmrStudioTextField(
            label = "رقم الهاتف",
            value = uiState.phone,
            onValueChange = viewModel::updatePhone,
            error = uiState.phoneError,
            placeholder = "+966 5X XXX XXXX",
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        AmrStudioTextField(
            label = "كلمة المرور",
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            error = uiState.passwordError,
            placeholder = "••••••••",
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        AmrStudioTextField(
            label = "تأكيد كلمة المرور",
            value = uiState.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            error = uiState.confirmPasswordError,
            placeholder = "••••••••",
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        // Privacy agreement
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.agreePrivacy,
                onCheckedChange = { viewModel.updatePrivacyAgreement(it) },
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.layout.Column {
                androidx.compose.ui.text.AnnotatedString.Builder().apply {
                    append("أوافق على ")
                    pushStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ))
                    append("سياسة الخصوصية")
                    pop()
                    append(" و ")
                    pushStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ))
                    append("شروط الاستخدام")
                    pop()
                }.toAnnotatedString().let { annotated ->
                    androidx.compose.material3.Text(
                        text = annotated,
                        fontSize = 13.sp,
                        onClick = { offset ->
                            annotated.getStringAnnotations("PRIVACY", offset, offset + 1).ifNotEmpty { showPrivacy.value = true }
                            annotated.getStringAnnotations("TERMS", offset, offset + 1).ifNotEmpty { showTerms.value = true }
                        }
                    )
                }
            }
        }

        if (uiState.privacyError != null) {
            Text(text = uiState.privacyError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 28.dp))
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        AmrStudioButton(
            text = if (uiState.isLoading) "جاري إنشاء الحساب..." else "إنشاء حساب",
            onClick = viewModel::validateAndRegister,
            enabled = !uiState.isLoading
        )
    }
}