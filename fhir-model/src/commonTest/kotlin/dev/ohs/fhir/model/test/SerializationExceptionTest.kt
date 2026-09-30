/*
 * Copyright 2025-2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.ohs.fhir.model.test

import dev.ohs.fhir.model.r4.Resource as R4Resource
import dev.ohs.fhir.model.r4b.Resource as R4bResource
import dev.ohs.fhir.model.r5.Resource as R5Resource
import io.kotest.core.spec.style.FunSpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

class SerializationExceptionTest :
  FunSpec({
    val questionnaireWithoutStatusJson =
      """
      {
        "resourceType": "Questionnaire",
        "title": "Sample"
      }
      """
        .trimIndent()

    val questionnaireWithoutLinkIdJson =
      """
      {
        "resourceType": "Questionnaire",
        "status": "draft",
        "item": [
          {
            "text": "Question"
          }
        ]
      }
      """
        .trimIndent()

    val patientWithoutNarrativeDivJson =
      """
      {
        "resourceType": "Patient",
        "text": {
          "status": "generated"
        }
      }
      """
        .trimIndent()

    val patientWithNullGivenJson =
      """
      {
        "resourceType": "Patient",
        "name": [
          {
            "given": ["Ann", null]
          }
        ]
      }
      """
        .trimIndent()

    val patientWithMismatchedGivenLengthsJson =
      """
      {
        "resourceType": "Patient",
        "name": [
          {
            "given": ["Ann"],
            "_given": [null, null]
          }
        ]
      }
      """
        .trimIndent()

    val searchParameterWithNullBaseJson =
      """
      {
        "resourceType": "SearchParameter",
        "url": "http://example.org/sp",
        "name": "x",
        "status": "draft",
        "description": "x",
        "code": "x",
        "base": ["Patient", null],
        "type": "token"
      }
      """
        .trimIndent()

    fun <TResource : Any> serializationExceptionTestSuite(
      fhirVersion: String,
      resourceSerializer: KSerializer<TResource>,
    ) {
      context("$fhirVersion Missing Required Properties") {
        test("missing required enum property throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, questionnaireWithoutStatusJson)
            }
          assertEquals("Missing required property 'status' on Questionnaire", exception.message)
        }

        test("missing required primitive property throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, questionnaireWithoutLinkIdJson)
            }
          assertEquals(
            "Missing required property 'linkId' on Questionnaire.Item",
            exception.message,
          )
        }

        test("missing required div property on Narrative throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, patientWithoutNarrativeDivJson)
            }
          assertEquals(
            "Missing required property 'div' on Narrative",
            exception.message,
          )
        }
      }

      context("$fhirVersion Empty Repeated Primitive Positions") {
        test("null primitive list entry without an id/extension throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, patientWithNullGivenJson)
            }
          assertEquals(
            "Element 'given' at index 1 on HumanName has neither a value nor an id/extension",
            exception.message,
          )
        }

        test("primitive list shorter than its _ array throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, patientWithMismatchedGivenLengthsJson)
            }
          assertEquals(
            "Element 'given' at index 1 on HumanName has neither a value nor an id/extension",
            exception.message,
          )
        }

        test("null enum list entry without an id/extension throws SerializationException") {
          val exception =
            assertFailsWith<SerializationException> {
              testJson.decodeFromString(resourceSerializer, searchParameterWithNullBaseJson)
            }
          assertEquals(
            "Element 'base' at index 1 on SearchParameter has neither a value nor an id/extension",
            exception.message,
          )
        }
      }
    }

    serializationExceptionTestSuite("R4", serializer<R4Resource>())
    serializationExceptionTestSuite("R4B", serializer<R4bResource>())
    serializationExceptionTestSuite("R5", serializer<R5Resource>())
  })
