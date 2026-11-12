# Test inventory

Observed source declarations; this inventory does not claim these tests were executed.

[Validation](../VALIDATION.md)

| Class | Source set | Example behavior from test method |
|---|---|---|
| AdministratorAccountTest | test | configured verified identity is administrator; same unverified email is a generic account |
| CampaignPayloadCodecTest | test | item structure and notes survive the room and delivery payload round trip; power and condition retain their structured fields |
| CatalogEntryRecordTest | test | structuredAbilityFieldsSurviveRoomMapping |
| CharacterMigrationManagerTest | test | fixturesFromDifferentAccountsMigrateIdempotently; firestoreMigrationCanPreserveCleanSynchronizationState |
| CharacterRecordFirestoreContractTest | test | canonicalPayloadsSurviveRoomAndFirestoreRecordRoundTrip; legacyBodyEditorChangesPreserveCanonicalInjuryHistory |
| DeliveryApplicationTest | test | the same delivery is applied exactly once across retries; a removed knowledge mapping cannot silently create a duplicate |
| BuiltInCatalogTest | test | containsAllPublishedExamplesAndPaths; idsAreStableAndUnique |
| EnhancementRulesTest | test | catalogsProvideThirtyFunctionalEntriesOfEachKind; gemAndTechnologyShareTheSameCapacity |
| EquipmentGlossaryTest | test | includesEveryRequiredRulesTerm; includesAllItemsMaterialsAndModifications |
| ItemCreationRulesTest | test | removing a prerequisite also removes dependent modifications; initialHeritageBudgetIsThirty |
| ItemNormalizationTest | test | invalidSecondaryMaterialIsRemovedWithoutChangingThePrimaryMaterial; heavyWeaponConsumesTwoHandsAndBlocksAnotherWieldedItem |
| KnowledgeProgressionTest | test | levelJumpWaitsForEveryCrossedMilestoneAndResolvesInOrder; cancelledTransitionDoesNotGrantOrphanReward |
| PathPresetsTest | test | everyBuiltInPathHasACompletePreset; selectingAPathFillsEverythingAndDoesNotDuplicateManagedPowers |
| PhaseOneCatalogTest | test | everyPublishedPowerCanBeAddedToTheCanonicalSheet; mysticCatalogOriginUsesRuleValuesInsteadOfInternalCatalogIds |
| RaceSelectionTest | test | empty subrace powers do not crash selection; catalogContainsCanonicalRacesAndRestrictsOrganicSubRaces |
| CharacterCreationTest | test | new character starts with one of five destiny; racial attribute bonus does not spend one of the ten creation points |
| AbilityManagementTest | test | ashBuilderCalculatesHeritageAndLoadFromPurityAndMergesDoses; ashAddedAfterCreationNeverReceivesHeritageCost |
| CampaignDeletionTest | test | deletedCampaignIsNeverActive; archivedCampaignIsNotDeleted |
| CharacterSyncConflictTest | test | automaticallySelectsOnlineFieldsForAtMostFiveNewerConflicts; automaticallyKeepsLocalFieldsForAtMostFiveNewerLocalConflicts |
| CharacterTest | test | typedProtectionItem; loadConditionAppliesOverloadedPenaltiesAndImmobility |
| OrganImplantTest | test | restorative implant clears failures only when applied; unlinking or relabelling an organ does not clear failures |
| PhaseOneCalculatedValuesTest | test | attributeCalculationIncludesAdjustmentAndEquippedItemOrigin; canonicalBasicKnowledgeTargetDoesNotAffectSimilarNames |
| PhaseTwoCampaignTest | test | legacy default campaign id becomes standalone; archived campaign exposes read only state |
| ProtectionBreakdownsTest | test | equippedArmorExplainsPgPlAndAgilityLimit; shieldPgOnlyAppliesWhenWielded |
| ResourceBreakdownsTest | test | lifeMaximumBreakdownKeepsItemBonusVisible; loadCapacityBreakdownIncludesStrengthItem |
| StructuredRulesTest | test | publishedRevisionIsImmutable; concurrentRevisionRejectsStaleWriter |
| UserProfileTest | test | firstNameUsesGoogleDisplayName; firstNameFallsBackToEmail |
| CharacterAccessPolicyTest | test | ownerCanEditAndDeleteUnlockedCharacter; lockedOwnerCanEditAndDeleteOwnCharacter |
| LevelProgressionTest | test | applies every intermediate level atomically; rejects incomplete selection without changing source character |
| AbilityCostFieldTest | test | spell and rune accept zero cost while ash keeps one dose minimum |
| CharacterDraftTest | test | stale repository emissions never replace newer typing |
| CharacterSheetPagerTest | test | mystic page is immediately to the right of powers; inventory page is immediately to the right of mystic |
| EquipmentRegionTest | test | pluralAndAccentedFootRegionMatchesBothFeet; otherPluralRegionsMatchTheirIndividualMembers |
| ItemEffectPresentationTest | test | typed effect uses a readable label; rule effect shows its description without leaking the enum |
| RemoteSyncPolicyTest | test | automaticSyncAllowsLifecycleAndIdleTriggers; disabledAutomaticSyncAllowsOnlyManualTrigger |
| AdaptiveLayoutPolicyTest | test | widthClassesChangeAtTabletAndExpandedBoundaries; shortLandscapeWindowsKeepReachableBottomNavigation |
| CyberGrungeMotionTest | test | edgeGlitchFollowsHorizontalMovement |
| CybergrungeRenderBudgetTest | test | experimentalCorruptionStaysInsideEmergencyCeiling; compositionBoundMotionStaysInsideTimingBudget |
| SdoControlComponentsTest | test | responsive grid uses one column on narrow phones; responsive grid grows on phones and tablets without overcompressing actions |
| SdoVisualModeTest | test | cybergrungeHasItsOwnIdentityAndPalette; standardInterfaceKeepsThemeSelection |
| AppDatabaseMigrationInstrumentedTest | androidTest | setUp; tearDown |
| CharacterCreationWizardResponsiveTest | androidTest | navigationStacksOnNarrowScreens; navigationSharesARowOnWideScreens |
| AdaptiveUiInstrumentedTest | androidTest | navigationSelectionRemainsReachableAndAnnounced; enlargedTextReflowsActionsWithoutLeavingViewport |
| CharacterSheetAccessInstrumentedTest | androidTest | cybergrungePreservesArchivedReadOnlyExplanation; standardPreservesArchivedReadOnlyExplanation |
| CyberGrungeComponentsInstrumentedTest | androidTest | emptyFieldUsesVisualSignalInsteadOfPlaceholderText; visualMissingSignalLeavesCompositionWhenFieldHasContent |
