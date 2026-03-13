package com.expense.tracker.di

import com.expense.tracker.data.repository.CategoryRepository
import com.expense.tracker.data.repository.CategoryRepositoryImpl
import com.expense.tracker.data.repository.ExpenseRepository
import com.expense.tracker.data.repository.ExpenseRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(firestore: FirebaseFirestore): ExpenseRepository {
        return ExpenseRepositoryImpl(firestore)
    }
    @Provides
    @Singleton
    fun provideCategoryRepository(firestore: FirebaseFirestore): CategoryRepository {
        return CategoryRepositoryImpl(firestore)
    }
}
