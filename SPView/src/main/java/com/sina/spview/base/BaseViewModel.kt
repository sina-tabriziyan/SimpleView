///**
// * Created by ST on 3/8/2025.
// * Author: Sina Tabriziyan
// * @sina.tabriziyan@gmail.com
// */
//package com.sina.spview.base
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.teamyar.core.constants.SharePrefConstant
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.receiveAsFlow
//import kotlinx.coroutines.launch
//import org.koin.core.component.KoinComponent
//import org.koin.core.component.inject
//
//open class BaseViewModel<I : Any, S : Any>(
//    initialState: S,
//) : ViewModel(), KoinComponent {
//
//    protected open val TAG: String get() = this::class.java.simpleName
//    private val languageSettingsHelper: LanguageSettingsHelper by inject()
//    private val _currentLanguage = MutableStateFlow(getCurrentLanguageSetting())
//    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
//    protected fun getCurrentLanguageSetting(): String {
//        val storedLang = languageSettingsHelper.getLanguage(SharePrefConstant.SHARED_SELECTED_LANGUAGE)
//        return storedLang.takeIf { it?.isNotBlank() == true } ?: getSystemLanguage()
//    }
//
//    protected fun saveLanguageSetting(newLang: String) {
//        languageSettingsHelper.saveLanguage(SharePrefConstant.SHARED_SELECTED_LANGUAGE, newLang)
//        _currentLanguage.value = newLang
//    }
//
//    // Helper function to get the system's language
//    private fun getSystemLanguage(): String {
//        return java.util.Locale.getDefault().language
//    }
//
//    protected val _state = MutableStateFlow(initialState)
//    val state: StateFlow<S> = _state.asStateFlow()
//
//    protected val _intents: Channel<I> = Channel(Channel.BUFFERED)
//    val intents: Flow<I> = _intents.receiveAsFlow()
//
//    protected fun launchIo(block: suspend () -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) { block() }
//    }
//
//    protected fun launchMain(block: suspend () -> Unit) {
//        viewModelScope.launch(Dispatchers.Main) { block() }
//    }
//
//    fun <T> MutableStateFlow<T>.update(update: T.() -> T) {
//        value = value.update()
//    }
//
//    protected fun findSidInHeaders(values: List<String>): String =
//        values.firstOrNull { it.startsWith("SID=") }?.substringBefore(";") ?: ""
//}
