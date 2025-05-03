///**
// * Created by ST on 3/8/2025.
// * Author: Sina Tabriziyan
// * @sina.tabriziyan@gmail.com
// */
//package com.sina.spview.base
//
//import android.content.Context
//import android.content.ContextWrapper
//import android.graphics.Typeface
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Parcelable
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import java.lang.reflect.ParameterizedType
//import kotlin.getValue
//import androidx.core.net.toUri
//import androidx.drawerlayout.widget.DrawerLayout
//import androidx.viewbinding.ViewBinding
//import java.util.Locale
//
//abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel<*, *>> : Fragment() {
//    protected open val TAG: String get() = this::class.java.simpleName
//
//    //    protected abstract val viewModel: VM
//    private var _binding: VB? = null
//    protected val binding get() = _binding!!
//    private val flowJobs = mutableListOf<Job>()
//    lateinit var navControllerFragment: NavController
//    private var currentToast: Toast? = null
//
//
//    protected val viewModel: VM by lazy {
//        val viewModelClass = (javaClass.genericSuperclass as ParameterizedType)
//            .actualTypeArguments[1] as Class<VM>
//        ViewModelProvider(this, object : ViewModelProvider.Factory {
//            override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                val handle = SavedStateHandle() // Obtain the actual SavedStateHandle
//                return getKoin().get(viewModelClass.kotlin) { parametersOf(handle) }
//            }
//        })[viewModelClass]
//    }
//
//    fun setNavController(controller: NavController) {
//        this.navControllerFragment = controller
//    }
//    abstract fun setupViews()
//    abstract fun observeViewModel()
//    private fun observeLanguageChanges() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewModel.currentLanguage.collect { newLanguage ->
//                Log.d("BaseFragment", "Language changed to: $newLanguage")
//                setLocale(newLanguage)
//                setLayoutDirection(newLanguage)
//                refreshUIForLanguage(newLanguage)
//            }
//        }
//    }
//    private fun setLayoutDirection(languageCode: String) {
//        binding.root.layoutDirection = if (languageCode == "fa") {
//            View.LAYOUT_DIRECTION_RTL
//        } else {
//            View.LAYOUT_DIRECTION_LTR
//        }
//    }
//    private fun setLocale(languageCode: String) {
//        val locale = Locale(languageCode)
//        Locale.setDefault(locale)
//        val config = resources.configuration
//        config.setLocale(locale)
//        resources.updateConfiguration(config, resources.displayMetrics)
//    }
//    open fun refreshUIForLanguage(language: String) {
//        // Optional: Implement in child Fragments if needed
//    }
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = inflateBinding(inflater, container)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        observeLanguageChanges()
//        setupViews()
//        observeViewModel()
//    }
//    private fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB {
//        val type =
//            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
//        return type.getMethod(
//            "inflate",
//            LayoutInflater::class.java,
//            ViewGroup::class.java,
//            Boolean::class.java
//        ).invoke(null, inflater, container, false) as VB
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
//        flowJobs.forEach { it.cancel() }
//        flowJobs.clear()
//        _binding = null
//    }
//
//    fun <T> launchWhenCreated(flow: Flow<T>, collector: suspend (T) -> Unit) {
//        flowJobs.add(viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
//                flow.collect { value -> collector(value) }
//            }
//        })
//    }
//
//    fun Fragment.navigate(
//        destination: Int,
//        bundle: Bundle? = null,
//        animationState: AnimationState? = null,
//        clearBackStack: Boolean? = false,
//        inclusive: Boolean? = true,
//        isNestedGraph: Boolean? = false
//    ) {
//        navigateInternal(destination, bundle, animationState, clearBackStack, inclusive, isNestedGraph)
//    }
//
//    fun Fragment.navigate(
//        deepLink: String,
//        arguments: Map<String, String>? = null,
//        animationState: AnimationState? = null,
//        clearBackStack: Boolean? = false,
//        inclusive: Boolean? = true,
//        isNestedGraph: Boolean? = false
//    ) {
//        var uriString = deepLink
//        if (!arguments.isNullOrEmpty()) {
//            val queryParams = arguments.entries.joinToString("&") { "${it.key}=${it.value}" }
//            uriString += if (deepLink.contains("?")) "&$queryParams" else "?$queryParams"
//        }
//        navigateInternal(uriString.toUri(), null, animationState, clearBackStack, inclusive, isNestedGraph)
//    }
//
//
//    private fun Fragment.navigateInternal(
//        destination: Any,
//        bundle: Bundle? = null,
//        animationState: AnimationState? = null,
//        clearBackStack: Boolean? = false,
//        inclusive: Boolean? = true,
//        isNestedGraph: Boolean? = false
//    ) {
//        val navController = findNavController()
//
//        if (isNestedGraph == true) {
//            val currentGraph = navController.graph.findNode(
//                navController.currentDestination?.id ?: return
//            ) as? NavGraph
//            val parentGraphId = currentGraph?.id ?: return
//            navController.navigate(parentGraphId)
//        }
//
//        val navOptionsBuilder = NavOptions.Builder()
//
//        if (clearBackStack == true) {
//            navOptionsBuilder.setPopUpTo(
//                navController.currentDestination?.id ?: return,
//                true
//            )
//        }
//
//        animationState?.let {
//            val (enter, exit) = when (it) {
//                AnimationState.FADE_IN_OUT -> android.R.anim.fade_in to android.R.anim.fade_out
//                AnimationState.FADE_OUT_IN -> android.R.anim.fade_in to android.R.anim.fade_out
//                AnimationState.TO_LEFT -> R.anim.slide_in_right to R.anim.slide_out_left
//                AnimationState.TO_RIGHT -> R.anim.slide_in_left to R.anim.slide_out_right
//            }
//
//            navOptionsBuilder.setEnterAnim(enter).setExitAnim(exit)
//
//            if (it == AnimationState.FADE_IN_OUT || it == AnimationState.FADE_OUT_IN) {
//                navOptionsBuilder.setPopEnterAnim(enter).setPopExitAnim(exit)
//            }
//        }
//
//        val navOptions = navOptionsBuilder.build()
//
//        when (destination) {
//            is Int -> navController.navigate(destination, bundle, navOptions)
//            is Uri -> navController.navigate(destination, navOptions)
//        }
//    }
////    fun Fragment.navigate(
////        destination: Any,
////        bundle: Bundle? = null,
////        animationState: AnimationState? = null,
////        clearBackStack: Boolean? = false,
////        inclusive: Boolean? = true,
////        isNestedGraph: Boolean? = false
////    ) {
////        val navController = findNavController()
////
////        // If navigating inside a nested graph, navigate to the parent graph first
////        if (isNestedGraph == true) {
////            val currentGraph = navController.graph.findNode(
////                navController.currentDestination?.id ?: return
////            ) as? NavGraph
////            val parentGraphId = currentGraph?.id ?: return
////            navController.navigate(parentGraphId)
////        }
////
////        // Build the NavOptions dynamically based on provided arguments
////        val navOptionsBuilder = NavOptions.Builder()
////
////        // Handle back stack clearing
////        if (clearBackStack == true) {
////            navOptionsBuilder.setPopUpTo(
////                navController.currentDestination?.id ?: return,
////                true // Inclusive: also pop the current fragment
////            )
////        }
////
////        // Handle animations if provided
////        animationState?.let {
////            val (enter, exit) = when (it) {
////                AnimationState.FADE_IN_OUT -> R.anim.slide_in_right to R.anim.slide_out_left
////                AnimationState.FADE_OUT_IN -> R.anim.slide_in_left to R.anim.slide_out_right
////                AnimationState.TO_LEFT -> R.anim.slide_in_right to R.anim.slide_out_left
////                AnimationState.TO_RIGHT -> R.anim.slide_in_left to R.anim.slide_out_right
////            }
////
////            navOptionsBuilder.setEnterAnim(enter).setExitAnim(exit)
////
////            if (it == AnimationState.FADE_IN_OUT || it == AnimationState.FADE_OUT_IN) {
////                navOptionsBuilder.setPopEnterAnim(enter).setPopExitAnim(exit)
////            }
////        }
////
////        val navOptions = navOptionsBuilder.build()
////
////        // Perform navigation based on destination type
////        when (destination) {
////            is Int -> navController.navigate(destination, bundle, navOptions)
////            is String -> navController.navigate(destination.toUri(), navOptions)
////        }
////    }
//
//    fun Fragment.navigateSafeArgs(
//        destinationId: Int,
//        args: Any? = null,  // Support Safe Args & Bundles
//        animationState: AnimationState? = null,
//        clearBackStack: Boolean = false,
//        inclusive: Boolean = true,
//        isNestedGraph: Boolean = false
//    ) {
//        val navController = findNavController()
//
//        // If navigating inside a nested graph, navigate to the parent graph first
//        if (isNestedGraph) {
//            val currentGraph = navController.graph.findNode(
//                navController.currentDestination?.id ?: return
//            ) as? NavGraph
//            val parentGraphId = currentGraph?.id ?: return
//            navController.navigate(parentGraphId)
//        }
//
//        // Convert Safe Args object to Bundle if needed
//        val bundle = when (args) {
//            is Bundle -> args
//            is Parcelable -> Bundle().apply { putParcelable("key", args) }
//            is ArrayList<*> -> Bundle().apply {
//                putStringArrayList(
//                    "key",
//                    args as ArrayList<String>
//                )
//            }
//
//            else -> null
//        }
//
//        // Validate if destination requires arguments
//        val destination = navController.graph.findNode(destinationId)
//        if (destination is NavDestination) {
//            val requiredArgs = destination.arguments
//            if (requiredArgs.isNotEmpty() && bundle == null) {
//                throw IllegalArgumentException("Destination $destinationId requires arguments but none were provided.")
//            }
//        }
//
//        // Build the NavOptions dynamically based on provided arguments
//        val navOptions = NavOptions.Builder().apply {
//            // Handle back stack clearing
//            if (clearBackStack) {
//                if (clearBackStack == true) inclusive.let {
//                    setPopUpTo(
//                        navController.graph.startDestinationId,
//                        it
//                    )
//                }
//            }
//
//            // Handle animations if provided
//            animationState?.let {
//                val (enter, exit) = when (it) {
//                    AnimationState.FADE_IN_OUT -> android.R.anim.fade_in to android.R.anim.fade_out
//                    AnimationState.FADE_OUT_IN -> android.R.anim.fade_out to android.R.anim.fade_in
//                    AnimationState.TO_LEFT -> R.anim.slide_in_right to R.anim.slide_out_left
//                    AnimationState.TO_RIGHT -> R.anim.slide_in_left to R.anim.slide_out_right
//                }
//
//                setEnterAnim(enter).setExitAnim(exit)
//
//                if (it == AnimationState.FADE_IN_OUT || it == AnimationState.FADE_OUT_IN) {
//                    setPopEnterAnim(enter).setPopExitAnim(exit)
//                }
//            }
//        }.build()
//
//        // Perform the final navigation with arguments
//        navController.navigate(destinationId, bundle ?: Bundle(), navOptions)
//    }
//
//    fun showToast(msgRes: String) {
//        // Cancel any existing toast to prevent overlap
//        currentToast?.cancel()
//
//        val typeface =
//            Typeface.createFromAsset(requireContext().assets, "IRANSansMobile(NoEn)_Light.ttf")
//        val inflater = LayoutInflater.from(requireContext())
//        val layout = inflater.inflate(R.layout.custom_toast, null)
//        val text = layout.findViewById<TextView>(R.id.txt_message)
//        text.typeface = typeface
//        text.text = msgRes
//
//        // Create a new toast and display it
//        currentToast = Toast(requireContext()).apply {
//            setGravity(Gravity.BOTTOM, 0, 100)
//            duration = Toast.LENGTH_SHORT
//            view = layout
//            show()
//        }
//    }
//
//    fun showToast(msgRes: Int) {
//        // Cancel any existing toast to prevent overlap
//        currentToast?.cancel()
//
//        val typeface =
//            Typeface.createFromAsset(requireContext().assets, "IRANSansMobile(NoEn)_Light.ttf")
//        val inflater = LayoutInflater.from(requireContext())
//        val layout = inflater.inflate(R.layout.custom_toast, null)
//        val text = layout.findViewById<TextView>(R.id.txt_message)
//        text.typeface = typeface
//        text.text = requireContext().getString(msgRes)
//
//        // Create a new toast and display it
//        currentToast = Toast(requireContext()).apply {
//            setGravity(Gravity.BOTTOM, 0, 100)
//            duration = Toast.LENGTH_SHORT
//            view = layout
//            show()
//        }
//    }
//
//    fun updateResources(context: Context, language: String): Context? {
//        val locale = Locale(language)
//        Locale.setDefault(locale)
//        return context.createConfigurationContext(context.resources.configuration.apply {
//            setLocale(locale)
//            setLayoutDirection(locale)
//        })
//    }
//
//    fun changeDirection(language: String?, mainLayout: ViewGroup) {
//        Log.e(TAG, "changeDirection language----------->>>>: $language", )
//        val locale = if (language.equals("fa", ignoreCase = true)) Locale("fa") else Locale(language)
//        Locale.setDefault(locale)
//        resources.configuration.setLocale(locale)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            requireActivity().createConfigurationContext(resources.configuration)
//
//        resources.updateConfiguration(resources.configuration, resources.displayMetrics)
//        language?.let { updateResources(requireContext(), it) }
//        ContextUtils.updateLocale(requireContext(), Locale(language))
//
//        val isRtl = language.equals("fa", ignoreCase = true)
//        Log.e(TAG, "changeDirection isRtl: $isRtl", )
//        requireActivity().window.decorView.layoutDirection =
//            if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
//        mainLayout.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
//    }
//
//}
//
//class ContextUtils(base: Context) : ContextWrapper(base) {
//    companion object {
//        fun updateLocale(context: Context, locale: Locale): ContextWrapper {
//            val configuration = context.resources.configuration
//            configuration.setLocale(locale)
//            return ContextUtils(context.createConfigurationContext(configuration))
//        }
//    }
//}
