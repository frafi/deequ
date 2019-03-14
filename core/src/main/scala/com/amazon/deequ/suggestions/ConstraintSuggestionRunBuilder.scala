/**
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.amazon.deequ.suggestions

import com.amazon.deequ.RepositoryOptions
import com.amazon.deequ.profiles.{ColumnProfile, ColumnProfiles}
import com.amazon.deequ.repository._
import com.amazon.deequ.runtime.{Dataset, Engine}
import com.amazon.deequ.suggestions.rules.ConstraintRule

/** A class to build a Constraint Suggestion run using a fluent API */
class ConstraintSuggestionRunBuilder(val dataset: Dataset) {

  protected var constraintRules: Seq[ConstraintRule[ColumnProfile]] = Seq.empty
  protected var printStatusUpdates: Boolean = false
  protected var testsetRatio: Option[Double] = None
  protected var testsetSplitRandomSeed: Option[Long] = None
  protected var cacheInputs: Boolean = false
  protected var lowCardinalityHistogramThreshold: Int = ColumnProfiles.DEFAULT_CARDINALITY_THRESHOLD
  protected var restrictToColumns: Option[Seq[String]] = None

  protected var metricsRepository: Option[MetricsRepository] = None
  protected var reuseExistingResultsKey: Option[ResultKey] = None
  protected var failIfResultsForReusingMissing: Boolean = false
  protected var saveOrAppendResultsKey: Option[ResultKey] = None

  protected var overwriteOutputFiles: Boolean = false
  protected var saveColumnProfilesJsonPath: Option[String] = None
  protected var saveConstraintSuggestionsJsonPath: Option[String] = None
  protected var saveEvaluationResultsJsonPath: Option[String] = None

  protected def this(constraintSuggestionRunBuilder: ConstraintSuggestionRunBuilder) {

    this(constraintSuggestionRunBuilder.dataset)

    constraintRules = constraintSuggestionRunBuilder.constraintRules
    printStatusUpdates = constraintSuggestionRunBuilder.printStatusUpdates
    testsetRatio = constraintSuggestionRunBuilder.testsetRatio
    testsetSplitRandomSeed = constraintSuggestionRunBuilder.testsetSplitRandomSeed
    cacheInputs = constraintSuggestionRunBuilder.cacheInputs
    lowCardinalityHistogramThreshold = constraintSuggestionRunBuilder
      .lowCardinalityHistogramThreshold
    restrictToColumns = constraintSuggestionRunBuilder.restrictToColumns

    metricsRepository = constraintSuggestionRunBuilder.metricsRepository
    reuseExistingResultsKey = constraintSuggestionRunBuilder.reuseExistingResultsKey
    failIfResultsForReusingMissing = constraintSuggestionRunBuilder.failIfResultsForReusingMissing
    saveOrAppendResultsKey = constraintSuggestionRunBuilder.saveOrAppendResultsKey

    overwriteOutputFiles = constraintSuggestionRunBuilder.overwriteOutputFiles
    saveColumnProfilesJsonPath = constraintSuggestionRunBuilder.saveColumnProfilesJsonPath
    saveConstraintSuggestionsJsonPath = constraintSuggestionRunBuilder
      .saveConstraintSuggestionsJsonPath
    saveEvaluationResultsJsonPath = constraintSuggestionRunBuilder.saveEvaluationResultsJsonPath
  }

  /**
    * Add a single rule for suggesting constraints based on ColumnProfiles to the run.
    *
    * @param constraintRule A rule ...
    */
  def addConstraintRule(constraintRule: ConstraintRule[ColumnProfile]): this.type = {
    constraintRules :+= constraintRule
    this
  }

  /**
    * Add multiple rules for suggesting constraints based on ColumnProfiles to the run.
    *
    * @param constraintRules A sequence of rules ...
    */
  def addConstraintRules(constraintRules: Seq[ConstraintRule[ColumnProfile]]): this.type = {
    this.constraintRules ++= constraintRules
    this
  }

  /**
    * Print status updates between passes
    *
    * @param printStatusUpdates Whether to print status updates
    */
  def printStatusUpdates(printStatusUpdates: Boolean): this.type = {
    this.printStatusUpdates = printStatusUpdates
    this
  }

  /**
    * Cache inputs
    *
    * @param cacheInputs Whether to cache inputs
    */
  def cacheInputs(cacheInputs: Boolean): this.type = {
    this.cacheInputs = cacheInputs
    this
  }

  /**
    * Run constraint suggestion on part of data and evaluate suggestions
    * on hold-out test set
    *
    * @param testsetRatio A value between 0 and 1
    */
  def useTrainTestSplitWithTestsetRatio(
      testsetRatio: Double,
      testsetSplitRandomSeed: Option[Long] = None)
    : this.type = {

    this.testsetRatio = Option(testsetRatio)
    this.testsetSplitRandomSeed = testsetSplitRandomSeed
    this
  }

  /**
    * Set the thresholds of values until it is considered to expensive to
    * calculate the histograms
    *
    * @param lowCardinalityHistogramThreshold The threshold
    */
  def withLowCardinalityHistogramThreshold(lowCardinalityHistogramThreshold: Int): this.type = {
    this.lowCardinalityHistogramThreshold = lowCardinalityHistogramThreshold
    this
  }

  /**
    * Can be used to specify a subset of columns to look at
    *
    * @param restrictToColumns can contain a subset of columns to profile and suggest constraints
    *                          for, otherwise all columns will be considered
    */
  def restrictToColumns(restrictToColumns: Seq[String]): this.type = {
    this.restrictToColumns = Option(restrictToColumns)
    this
  }

  /**
    * Set a metrics repository associated with the current data to enable features like reusing
    * previously computed results and storing the results of the current run.
    *
    * @param metricsRepository A metrics repository to store and load results associated with the
    *                          run
    */
  def useRepository(metricsRepository: MetricsRepository)
    : ConstraintSuggestionRunBuilderWithRepository = {

    new ConstraintSuggestionRunBuilderWithRepository(this, Option(metricsRepository))
  }

  def run(): ConstraintSuggestionResult = {
    ConstraintSuggestionRunner().run(
      dataset,
      constraintRules,
      restrictToColumns,
      lowCardinalityHistogramThreshold,
      printStatusUpdates,
      testsetRatio,
      testsetSplitRandomSeed,
//      cacheInputs,
//      ConstraintSuggestionFileOutputOptions(
//        sparkSession,
//        saveColumnProfilesJsonPath,
//        saveConstraintSuggestionsJsonPath,
//        saveEvaluationResultsJsonPath,
//        overwriteOutputFiles),
      RepositoryOptions(
        metricsRepository,
        reuseExistingResultsKey,
        failIfResultsForReusingMissing,
        saveOrAppendResultsKey)
    )
  }
}

class ConstraintSuggestionRunBuilderWithRepository(
    constraintSuggestionRunBuilder: ConstraintSuggestionRunBuilder,
    usingMetricsRepository: Option[MetricsRepository])
  extends ConstraintSuggestionRunBuilder(constraintSuggestionRunBuilder) {

  metricsRepository = usingMetricsRepository

   /**
    * Reuse any previously computed results stored in the metrics repository associated with the
    * current data to save computation time.
    *
    * @param resultKey The exact result key of the previously computed result
    */
  def reuseExistingResultsForKey(
      resultKey: ResultKey,
      failIfResultsMissing: Boolean = false)
    : this.type = {

    reuseExistingResultsKey = Option(resultKey)
    failIfResultsForReusingMissing = failIfResultsMissing
    this
  }

  /**
    * A shortcut to save the results of the run or append them to existing results in the
    * metrics repository.
    *
    * @param resultKey The result key to identify the current run
    */
  def saveOrAppendResult(resultKey: ResultKey): this.type = {
    saveOrAppendResultsKey = Option(resultKey)
    this
  }
}

//class ConstraintSuggestionRunBuilderWithSparkSession(
//    constraintSuggestionRunBuilder: ConstraintSuggestionRunBuilder,
//    usingSparkSession: Option[SparkSession])
//  extends ConstraintSuggestionRunBuilder(constraintSuggestionRunBuilder) {
//
//  sparkSession = usingSparkSession
//
//  /**
//    * Save the column profiles json to e.g. S3
//    *
//    * @param path The file path
//    */
//  def saveColumnProfilesJsonToPath(
//      path: String)
//    : this.type = {
//
//    saveColumnProfilesJsonPath = Option(path)
//    this
//  }
//
//  /**
//    * Save the constraint suggestion json to e.g. S3
//    *
//    * @param path The file path
//    */
//  def saveConstraintSuggestionsJsonToPath(
//      path: String)
//    : this.type = {
//
//    saveConstraintSuggestionsJsonPath = Option(path)
//    this
//  }
//
//  /**
//    * Save the evaluation results json to e.g. S3
//    *
//    * @param path The file path
//    */
//  def saveEvaluationResultsJsonToPath(
//      path: String)
//    : this.type = {
//
//    saveEvaluationResultsJsonPath = Option(path)
//    this
//  }
//
//  /**
//    * Whether previous files with identical names should be overwritten when
//    * saving files to some file system.
//    *
//    * @param overwriteFiles Whether previous files with identical names
//    *                       should be overwritten
//    */
//  def overwritePreviousFiles(overwriteFiles: Boolean): this.type = {
//    overwriteOutputFiles = overwriteOutputFiles
//    this
//  }
//}
