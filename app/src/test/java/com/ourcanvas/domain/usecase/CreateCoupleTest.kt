package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class CreateCoupleTest {

    private lateinit var mockRepository: CanvasRepository
    private lateinit var createCouple: CreateCouple

    @Before
    fun setUp() {
        mockRepository = Mockito.mock(CanvasRepository::class.java)
        createCouple = CreateCouple(mockRepository)
    }

    @Test
    fun `invoke should return success with coupleId`() = runBlocking {
        // Given
        val uid = "test_uid"
        val expectedCoupleId = "test_couple_id"
        `when`(mockRepository.createCouple(uid)).thenReturn(Result.success(expectedCoupleId))

        // When
        val result = createCouple(uid)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedCoupleId, result.getOrNull())
    }

    @Test
    fun `invoke should return failure when repository fails`() = runBlocking {
        // Given
        val uid = "test_uid"
        val exception = Exception("Test exception")
        `when`(mockRepository.createCouple(uid)).thenReturn(Result.failure(exception))

        // When
        val result = createCouple(uid)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}