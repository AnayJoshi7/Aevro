package com.anay.fitnesstracker

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anay.fitnesstracker.Screens.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.Screens.PersonalInfoScreen
import com.anay.fitnesstracker.Screens.NotificationScreen
import com.anay.fitnesstracker.Screens.AboutScreen
import androidx.navigation.NavType

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_CONTINUED = "onboarding_continued"
    const val PREFERENCES = "preferences"
    const val WORKOUT_SPLIT = "workout_split"
    const val CONFIRMATION = "confirmation"
    const val DASHBOARD = "dashboard"
    const val WORKOUTS = "workouts"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val MEAL_LOG = "meal_log"

    const val PERSONAL_INFO = "personal_info"
    const val NOTIFICATIONS = "notifications"
    const val ABOUT = "about"

    const val WORKOUT_PUSH_DAY = "workout_push_day"
    const val WORKOUT_PULL_DAY = "workout_pull_day"
    const val WORKOUT_LEG_DAY = "workout_leg_day"
    const val WORKOUT_UPPER_BODY = "workout_upper_body"
    const val WORKOUT_LOWER_BODY = "workout_lower_body"
    const val WORKOUT_FULL_BODY = "workout_full_body"

    const val LOG_WORKOUT = "log_workout"
    const val CUSTOM_SCHEDULE = "custom_schedule"
    const val ADD_EXERCISES = "add_exercises/{dayName}"
    const val CUSTOM_WORKOUT_DETAIL = "custom_workout_detail/{dayOrWorkoutName}"
}
@Composable
fun FitnessAppNavHost(
    fitnessViewModel: FitnessViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isCheckingSession by fitnessViewModel.isSessionChecking.collectAsState()
    val currentUser by fitnessViewModel.currentUser.collectAsState()

    if (isCheckingSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF131416)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF27D07F))
        }
        return
    }

    // Auto-login destination: If user is already saved, go directly to Dashboard
    val startDest = if (currentUser != null) Routes.DASHBOARD else Routes.SPLASH

    NavHost(
        navController = navController,
        startDestination = startDest,

        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                viewModel = fitnessViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateSignUp = { navController.navigate(Routes.ONBOARDING) }
            )
        }
        // --- Missing Sub-pages ---
        composable(Routes.PERSONAL_INFO) {
            PersonalInfoScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.CUSTOM_WORKOUT_DETAIL,
            arguments = listOf(navArgument("dayOrWorkoutName") { type = NavType.StringType })
        ) { backStackEntry ->
            val dayOrWorkoutName = backStackEntry.arguments?.getString("dayOrWorkoutName") ?: "Monday"
            CustomWorkoutDetailScreen(
                dayOrWorkoutName = dayOrWorkoutName,
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = fitnessViewModel,
                onNext = { navController.navigate(Routes.ONBOARDING_CONTINUED) }
            )
        }

        composable(Routes.ONBOARDING_CONTINUED) {
            OnboardingContinuedScreen(
                viewModel = fitnessViewModel,
                onNext = { navController.navigate(Routes.PREFERENCES) }
            )
        }

        composable(Routes.PREFERENCES) {
            PreferencesScreen(
                viewModel = fitnessViewModel,
                onNavigateWorkoutSplit = { navController.navigate(Routes.WORKOUT_SPLIT) },
                onNext = { navController.navigate(Routes.CONFIRMATION) }
            )
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(
                onNavigate = { route -> navController.navigate(route) },
                fitnessViewModel = fitnessViewModel
            )
        }

        composable(Routes.LOG_WORKOUT) {
            LogWorkoutScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Workout day screens
        composable(Routes.WORKOUT_PUSH_DAY) {
            PushDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_PULL_DAY) {
            PullDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_LEG_DAY) {
            LegDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_UPPER_BODY) {
            UpperBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_LOWER_BODY) {
            LowerBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_FULL_BODY) {
            FullBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }

        composable(Routes.WORKOUT_SPLIT) {
            WorkoutSplitScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONFIRMATION) {
            ConfirmationScreen(
                viewModel = fitnessViewModel,
                onEnterDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { route: String -> navController.navigate(route) },
                onNavigateMealLog = { navController.navigate(Routes.MEAL_LOG) },
                fitnessViewModel = fitnessViewModel
            )
        }

        composable(Routes.WORKOUTS) {
            WorkoutScreen(
                onNavigate = { route -> navController.navigate(route) },
                fitnessViewModel = fitnessViewModel
            )
        }

        composable(Routes.PROGRESS) {
            ProgressScreen(onNavigate = { route: String -> navController.navigate(route) },
                fitnessViewModel = fitnessViewModel)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigate = { route: String -> navController.navigate(route) },
                fitnessViewModel = fitnessViewModel,
                onLogout = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MEAL_LOG) {
            MealLogScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }


        composable(Routes.WORKOUT_PUSH_DAY) {
            PushDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_PULL_DAY) {
            PullDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_LEG_DAY) {
            LegDayScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_UPPER_BODY) {
            UpperBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_LOWER_BODY) {
            LowerBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.WORKOUT_FULL_BODY) {
            FullBodyScreen(
                onBack = { navController.popBackStack() },
                onNavigateLogWorkout = { navController.navigate(Routes.LOG_WORKOUT) }
            )
        }
        composable(Routes.CUSTOM_SCHEDULE) {
            CustomScheduleScreen(
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() },
                onNavigateAddExercises = { dayName ->
                    navController.navigate("add_exercises/$dayName")
                }
            )
        }

        composable(
            route = Routes.ADD_EXERCISES,
            arguments = listOf(androidx.navigation.navArgument("dayName") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val dayName = backStackEntry.arguments?.getString("dayName") ?: "Monday"
            AddExercisesScreen(
                dayName = dayName,
                viewModel = fitnessViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}