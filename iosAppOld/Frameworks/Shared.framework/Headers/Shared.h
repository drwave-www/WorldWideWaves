#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class NSBundle, NSLocale, SharedAbstractEventMap<T>, SharedAppScreen, SharedAppScreenAbout, SharedAppScreenEventDetails, SharedAppScreenEventsList, SharedAppScreenMap, SharedAppScreenWave, SharedAudioBufferFactory, SharedBackgroundTaskUsage, SharedBaseMapDownloadViewModelCompanion, SharedBaseViewModel, SharedBatteryUsage, SharedBoundingBox, SharedBoundingBoxCompanion, SharedBundleInitializer, SharedByteProcessing, SharedCheckEventFavoritesUseCase, SharedChoreography, SharedChoreographyDefinition, SharedChoreographyDefinitionCompanion, SharedChoreographyManagerDisplayableSequence<T>, SharedChoreographyManagerResolvedChoreography<T>, SharedChoreographyManagerResolvedSequence<T>, SharedChoreographySequence, SharedChoreographySequenceCompanion, SharedCityMap, SharedCityMapRegistry, SharedCityMapStatistics, SharedComposedLongitude, SharedComposedLongitudeCompanion, SharedComposedLongitudeOrientation, SharedComposedLongitudeSide, SharedCutPosition, SharedDateTimeFormats, SharedDefaultPositionObserverCompanion, SharedEarthAdaptedSpeedLongitudeLatLonBand, SharedEventFilterCriteria, SharedEventMapConfig, SharedEventState, SharedEventStateInput, SharedEventState_, SharedEventsFilterCallbacks, SharedEventsFilterState, SharedEventsViewModelCompanion, SharedFavoriteEventsStore, SharedFilterEventsUseCase, SharedGeoUtils, SharedGeoUtilsVector2D, SharedGetSortedEventsUseCase, SharedHiddenMapsStoreCompanion, SharedIClockCompanion, SharedIOSPlatformMapManagerCompanion, SharedIWWWEventStatus, SharedIWWWEventWaveNumbersLiterals, SharedKoin_coreBeanDefinition<T>, SharedKoin_coreCallbacks<T>, SharedKoin_coreCoreResolver, SharedKoin_coreExtensionManager, SharedKoin_coreInstanceFactory<T>, SharedKoin_coreInstanceFactoryCompanion, SharedKoin_coreInstanceRegistry, SharedKoin_coreKind, SharedKoin_coreKoin, SharedKoin_coreKoinDefinition<R>, SharedKoin_coreLevel, SharedKoin_coreLockable, SharedKoin_coreLogger, SharedKoin_coreModule, SharedKoin_coreOptionRegistry, SharedKoin_coreParametersHolder, SharedKoin_corePropertyRegistry, SharedKoin_coreResolutionContext, SharedKoin_coreScope, SharedKoin_coreScopeDSL, SharedKoin_coreScopeRegistry, SharedKoin_coreScopeRegistryCompanion, SharedKoin_coreSingleInstanceFactory<T>, SharedKoin_coreTypeQualifier, SharedKotlinAbstractCoroutineContextElement, SharedKotlinAbstractCoroutineContextKey<B, E>, SharedKotlinArray<T>, SharedKotlinByteArray, SharedKotlinByteIterator, SharedKotlinCancellationException, SharedKotlinDoubleArray, SharedKotlinDoubleIterator, SharedKotlinEnum<E>, SharedKotlinEnumCompanion, SharedKotlinException, SharedKotlinFloatArray, SharedKotlinFloatIterator, SharedKotlinIllegalStateException, SharedKotlinInstant, SharedKotlinInstantCompanion, SharedKotlinIntArray, SharedKotlinIntIterator, SharedKotlinIntProgression, SharedKotlinIntProgressionCompanion, SharedKotlinIntRange, SharedKotlinIntRangeCompanion, SharedKotlinLazyThreadSafetyMode, SharedKotlinMatchGroup, SharedKotlinMatchResultDestructured, SharedKotlinNothing, SharedKotlinPair<__covariant A, __covariant B>, SharedKotlinRegex, SharedKotlinRegexCompanion, SharedKotlinRegexOption, SharedKotlinRuntimeException, SharedKotlinShortArray, SharedKotlinShortIterator, SharedKotlinThrowable, SharedKotlinUnit, SharedKotlinx_coroutines_coreCoroutineDispatcher, SharedKotlinx_coroutines_coreCoroutineDispatcherKey, SharedKotlinx_datetimeDayOfWeek, SharedKotlinx_datetimeDayOfWeekNames, SharedKotlinx_datetimeDayOfWeekNamesCompanion, SharedKotlinx_datetimeFixedOffsetTimeZone, SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion, SharedKotlinx_datetimeLocalDate, SharedKotlinx_datetimeLocalDateCompanion, SharedKotlinx_datetimeLocalDateProgression, SharedKotlinx_datetimeLocalDateProgressionCompanion, SharedKotlinx_datetimeLocalDateRange, SharedKotlinx_datetimeLocalDateRangeCompanion, SharedKotlinx_datetimeLocalDateTime, SharedKotlinx_datetimeLocalDateTimeCompanion, SharedKotlinx_datetimeLocalTime, SharedKotlinx_datetimeLocalTimeCompanion, SharedKotlinx_datetimeMonth, SharedKotlinx_datetimeMonthNames, SharedKotlinx_datetimeMonthNamesCompanion, SharedKotlinx_datetimeOverloadMarker, SharedKotlinx_datetimePadding, SharedKotlinx_datetimeTimeZone, SharedKotlinx_datetimeTimeZoneCompanion, SharedKotlinx_datetimeUtcOffset, SharedKotlinx_datetimeUtcOffsetCompanion, SharedKotlinx_serialization_coreSerialKind, SharedKotlinx_serialization_coreSerializersModule, SharedKotlinx_serialization_jsonJsonElement, SharedKotlinx_serialization_jsonJsonElementCompanion, SharedLibraryDrawableResource, SharedLibraryFontResource, SharedLibraryPluralStringResource, SharedLibraryResource, SharedLibraryResourceItem, SharedLibraryStringArrayResource, SharedLibraryStringResource, SharedLog, SharedLogConfig, SharedMapCameraPosition, SharedMapConstraintManagerVisibleRegionPadding, SharedMapDownloadUtils, SharedMapDownloadUtilsRetryManager, SharedMapDownloadUtilsRetryManagerCompanion, SharedMapFeatureState, SharedMapFeatureStateAvailable, SharedMapFeatureStateCanceling, SharedMapFeatureStateDownloading, SharedMapFeatureStateFailed, SharedMapFeatureStateInstalled, SharedMapFeatureStateInstalling, SharedMapFeatureStateNotAvailable, SharedMapFeatureStateNotChecked, SharedMapFeatureStatePending, SharedMapFeatureStateRetrying, SharedMapFeatureStateUnknown, SharedMaterial3ColorScheme, SharedMaterial3Typography, SharedMidi, SharedMidiNote, SharedMidiParser, SharedMidiResources, SharedMidiTrack, SharedMokoRes, SharedMokoResStrings, SharedObservationPhase, SharedObservationSchedule, SharedPerformanceIssue, SharedPerformanceIssueCategory, SharedPerformanceIssueSeverity, SharedPerformanceMetrics, SharedPerformanceReport, SharedPolygon, SharedPolygonCompanion, SharedPolygonUtils, SharedPolygonUtilsQuad<A, B, C, D>, SharedPolygonUtilsSplitResult, SharedPolygonUtilsSplitResultCompanion, SharedPosition, SharedPositionCompanion, SharedPositionManager, SharedPositionManagerPositionSource, SharedPositionObservation, SharedProgressionSnapshot, SharedRes, SharedResArray, SharedResDrawable, SharedResFont, SharedResPlurals, SharedResString, SharedResourcesResourcePlatformDetails, SharedResourcesStringResource, SharedSegment, SharedSharedColorFamily, SharedSharedExtendedColorScheme, SharedSkikoBackendRenderTarget, SharedSkikoBackendRenderTargetCompanion, SharedSkikoBitmap, SharedSkikoBitmapCompanion, SharedSkikoBlendMode, SharedSkikoCanvas, SharedSkikoCanvasCompanion, SharedSkikoCanvasSaveLayerFlags, SharedSkikoCanvasSaveLayerFlagsSet, SharedSkikoCanvasSaveLayerRec, SharedSkikoClipMode, SharedSkikoColor4f, SharedSkikoColor4fCompanion, SharedSkikoColorAlphaType, SharedSkikoColorChannel, SharedSkikoColorFilter, SharedSkikoColorFilterCompanion, SharedSkikoColorInfo, SharedSkikoColorInfoCompanion, SharedSkikoColorMatrix, SharedSkikoColorSpace, SharedSkikoColorSpaceCompanion, SharedSkikoColorType, SharedSkikoColorTypeCompanion, SharedSkikoContentChangeMode, SharedSkikoData, SharedSkikoDataCompanion, SharedSkikoDirectContext, SharedSkikoDirectContextCompanion, SharedSkikoDrawable, SharedSkikoDrawableCompanion, SharedSkikoEncodedImageFormat, SharedSkikoFilterBlurMode, SharedSkikoFilterMode, SharedSkikoFilterTileMode, SharedSkikoFont, SharedSkikoFontCompanion, SharedSkikoFontEdging, SharedSkikoFontFamilyName, SharedSkikoFontFeature, SharedSkikoFontFeatureCompanion, SharedSkikoFontHinting, SharedSkikoFontMetrics, SharedSkikoFontMetricsCompanion, SharedSkikoFontMgr, SharedSkikoFontMgrCompanion, SharedSkikoFontSlant, SharedSkikoFontStyle, SharedSkikoFontStyleCompanion, SharedSkikoFontStyleSet, SharedSkikoFontStyleSetCompanion, SharedSkikoFontVariation, SharedSkikoFontVariationAxis, SharedSkikoFontVariationCompanion, SharedSkikoGLBackendState, SharedSkikoGradientStyle, SharedSkikoGradientStyleCompanion, SharedSkikoIPoint, SharedSkikoIPointCompanion, SharedSkikoIRect, SharedSkikoIRectCompanion, SharedSkikoISize, SharedSkikoISizeCompanion, SharedSkikoImage, SharedSkikoImageCompanion, SharedSkikoImageFilter, SharedSkikoImageFilterCompanion, SharedSkikoImageInfo, SharedSkikoImageInfoCompanion, SharedSkikoInversionMode, SharedSkikoManaged, SharedSkikoMaskFilter, SharedSkikoMaskFilterCompanion, SharedSkikoMatcher, SharedSkikoMatrix22, SharedSkikoMatrix22Companion, SharedSkikoMatrix33, SharedSkikoMatrix33Companion, SharedSkikoMatrix44, SharedSkikoMatrix44Companion, SharedSkikoNative, SharedSkikoNativeCompanion, SharedSkikoPaint, SharedSkikoPaintCompanion, SharedSkikoPaintMode, SharedSkikoPaintStrokeCap, SharedSkikoPaintStrokeJoin, SharedSkikoPath, SharedSkikoPathCompanion, SharedSkikoPathDirection, SharedSkikoPathEffect, SharedSkikoPathEffectCompanion, SharedSkikoPathEffectStyle, SharedSkikoPathEllipseArc, SharedSkikoPathFillMode, SharedSkikoPathOp, SharedSkikoPathSegment, SharedSkikoPathSegmentIterator, SharedSkikoPathSegmentIteratorCompanion, SharedSkikoPathVerb, SharedSkikoPattern, SharedSkikoPicture, SharedSkikoPictureCompanion, SharedSkikoPixelGeometry, SharedSkikoPixelRef, SharedSkikoPixelRefCompanion, SharedSkikoPixmap, SharedSkikoPixmapCompanion, SharedSkikoPoint, SharedSkikoPointCompanion, SharedSkikoRRect, SharedSkikoRRectCompanion, SharedSkikoRSXform, SharedSkikoRSXformCompanion, SharedSkikoRect, SharedSkikoRectCompanion, SharedSkikoRefCnt, SharedSkikoRegion, SharedSkikoRegionCompanion, SharedSkikoRegionOp, SharedSkikoRegionOpCompanion, SharedSkikoRuntimeEffect, SharedSkikoRuntimeEffectCompanion, SharedSkikoRuntimeShaderBuilder, SharedSkikoRuntimeShaderBuilderCompanion, SharedSkikoShader, SharedSkikoShaderCompanion, SharedSkikoShapingOptions, SharedSkikoShapingOptionsCompanion, SharedSkikoSurface, SharedSkikoSurfaceColorFormat, SharedSkikoSurfaceCompanion, SharedSkikoSurfaceOrigin, SharedSkikoSurfaceProps, SharedSkikoTextBlob, SharedSkikoTextBlobCompanion, SharedSkikoTextLine, SharedSkikoTextLineCompanion, SharedSkikoTypeface, SharedSkikoTypefaceCompanion, SharedSkikoVertexMode, SharedSoundChoreographyManager, SharedSoundPlayerWaveform, SharedStateValidationIssue, SharedStateValidationIssueSeverity, SharedTabConfiguration, SharedUi_graphicsBrush, SharedUi_graphicsBrushCompanion, SharedUi_graphicsColorFilter, SharedUi_graphicsColorFilterCompanion, SharedUi_graphicsDrawStyle, SharedUi_graphicsShadow, SharedUi_graphicsShadowCompanion, SharedUi_textFontFamily, SharedUi_textFontFamilyCompanion, SharedUi_textFontHinting, SharedUi_textFontRasterizationSettings, SharedUi_textFontRasterizationSettingsCompanion, SharedUi_textFontSmoothing, SharedUi_textFontWeight, SharedUi_textFontWeightCompanion, SharedUi_textGenericFontFamily, SharedUi_textLineHeightStyle, SharedUi_textLineHeightStyleCompanion, SharedUi_textLocale, SharedUi_textLocaleCompanion, SharedUi_textLocaleList, SharedUi_textLocaleListCompanion, SharedUi_textParagraphStyle, SharedUi_textPlatformParagraphStyle, SharedUi_textPlatformParagraphStyleCompanion, SharedUi_textPlatformSpanStyle, SharedUi_textPlatformSpanStyleCompanion, SharedUi_textPlatformTextStyle, SharedUi_textSpanStyle, SharedUi_textSystemFontFamily, SharedUi_textTextDecoration, SharedUi_textTextDecorationCompanion, SharedUi_textTextGeometricTransform, SharedUi_textTextGeometricTransformCompanion, SharedUi_textTextIndent, SharedUi_textTextIndentCompanion, SharedUi_textTextMotion, SharedUi_textTextMotionCompanion, SharedUi_textTextStyle, SharedUi_textTextStyleCompanion, SharedWWWEvent, SharedWWWEventArea, SharedWWWEventAreaCompanion, SharedWWWEventCompanion, SharedWWWEventMap, SharedWWWEventMapCompanion, SharedWWWEventObserver, SharedWWWEventObserverEventObservation, SharedWWWEventWWWWaveDefinition, SharedWWWEventWWWWaveDefinitionCompanion, SharedWWWEventWave, SharedWWWEventWaveCompanion, SharedWWWEventWaveDeep, SharedWWWEventWaveDeepCompanion, SharedWWWEventWaveDirection, SharedWWWEventWaveLinear, SharedWWWEventWaveLinearCompanion, SharedWWWEventWaveLinearSplit, SharedWWWEventWaveLinearSplitCompanion, SharedWWWEventWaveWarming, SharedWWWEventWaveWavePolygons, SharedWWWEvents, SharedWWWGlobals, SharedWWWGlobalsAudio, SharedWWWGlobalsBackNav, SharedWWWGlobalsByteProcessing, SharedWWWGlobalsChoreography, SharedWWWGlobalsCommon, SharedWWWGlobalsDimensions, SharedWWWGlobalsDisplayText, SharedWWWGlobalsEvent, SharedWWWGlobalsEventsList, SharedWWWGlobalsFAQ, SharedWWWGlobalsFileSystem, SharedWWWGlobalsGeodetic, SharedWWWGlobalsInfo, SharedWWWGlobalsLocationAccuracy, SharedWWWGlobalsMapDisplay, SharedWWWGlobalsMidi, SharedWWWGlobalsPerformanceThresholds, SharedWWWGlobalsSpatialIndex, SharedWWWGlobalsTabBar, SharedWWWGlobalsTiming, SharedWWWGlobalsUrls, SharedWWWGlobalsWave, SharedWWWGlobalsWaveDisplay, SharedWWWGlobalsWaveTiming, SharedWWWLogger, SharedWWWPlatform, SharedWWWSimulation, SharedWWWSimulationCompanion, SharedWaveState, SharedWaveformGenerator, UIImage, UIViewController;

@protocol SharedAudioBuffer, SharedCoroutineScopeProvider, SharedDataStoreFactory, SharedDataValidator, SharedDatastore_coreDataStore, SharedEventStateManager, SharedEventsConfigurationProvider, SharedEventsDecoder, SharedEventsRepository, SharedGeoJsonDataProvider, SharedIClock, SharedIMapDownloadManager, SharedIOSLifecycleObserver, SharedIOSObservable, SharedIOSObservableSubscription, SharedIOSReactiveSubscriptionManager, SharedIPerformanceMonitor, SharedIWWWEvent, SharedImageResolver, SharedKoin_coreKoinComponent, SharedKoin_coreKoinExtension, SharedKoin_coreKoinScopeComponent, SharedKoin_coreQualifier, SharedKoin_coreResolutionExtension, SharedKoin_coreScopeCallback, SharedKotlinAnnotation, SharedKotlinAppendable, SharedKotlinClosedRange, SharedKotlinCollection, SharedKotlinComparable, SharedKotlinContinuation, SharedKotlinContinuationInterceptor, SharedKotlinCoroutineContext, SharedKotlinCoroutineContextElement, SharedKotlinCoroutineContextKey, SharedKotlinFunction, SharedKotlinIterable, SharedKotlinIterator, SharedKotlinKAnnotatedElement, SharedKotlinKClass, SharedKotlinKClassifier, SharedKotlinKDeclarationContainer, SharedKotlinLazy, SharedKotlinMatchGroupCollection, SharedKotlinMatchResult, SharedKotlinMutableIterator, SharedKotlinOpenEndRange, SharedKotlinSequence, SharedKotlinSuspendFunction1, SharedKotlinx_coroutines_coreChildHandle, SharedKotlinx_coroutines_coreChildJob, SharedKotlinx_coroutines_coreCoroutineScope, SharedKotlinx_coroutines_coreDisposableHandle, SharedKotlinx_coroutines_coreFlow, SharedKotlinx_coroutines_coreFlowCollector, SharedKotlinx_coroutines_coreJob, SharedKotlinx_coroutines_coreMutableSharedFlow, SharedKotlinx_coroutines_coreMutableStateFlow, SharedKotlinx_coroutines_coreParentJob, SharedKotlinx_coroutines_coreRunnable, SharedKotlinx_coroutines_coreSelectClause, SharedKotlinx_coroutines_coreSelectClause0, SharedKotlinx_coroutines_coreSelectInstance, SharedKotlinx_coroutines_coreSharedFlow, SharedKotlinx_coroutines_coreStateFlow, SharedKotlinx_datetimeDateTimeFormat, SharedKotlinx_datetimeDateTimeFormatBuilder, SharedKotlinx_datetimeDateTimeFormatBuilderWithDate, SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime, SharedKotlinx_datetimeDateTimeFormatBuilderWithTime, SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset, SharedKotlinx_datetimeDateTimeFormatBuilderWithYearMonth, SharedKotlinx_serialization_coreCompositeDecoder, SharedKotlinx_serialization_coreCompositeEncoder, SharedKotlinx_serialization_coreDecoder, SharedKotlinx_serialization_coreDeserializationStrategy, SharedKotlinx_serialization_coreEncoder, SharedKotlinx_serialization_coreKSerializer, SharedKotlinx_serialization_coreSerialDescriptor, SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreSerializersModuleCollector, SharedLibraryQualifier, SharedMapAvailabilityChecker, SharedMapCameraCallback, SharedMapDataProvider, SharedMapLibreAdapter, SharedObservationScheduler, SharedPerformanceTrace, SharedPlatformMapManager, SharedPolygonLoopIterator, SharedPositionObserver, SharedResourcesResourceContainer, SharedSkikoIHasImageInfo, SharedSkikoSamplingMode, SharedSoundPlayer, SharedUi_graphicsPaint, SharedUi_graphicsPathEffect, SharedUi_textAnnotatedStringAnnotation, SharedVolumeController, SharedWWWLocationProvider, SharedWaveProgressionTracker;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SharedBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedBase (SharedBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SharedByte : SharedNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedUByte : SharedNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedShort : SharedNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedUShort : SharedNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedInt : SharedNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedUInt : SharedNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedLong : SharedNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedULong : SharedNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedFloat : SharedNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedDouble : SharedNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedBoolean : SharedNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BundleInitializer")))
@interface SharedBundleInitializer : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bundleInitializer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBundleInitializer *shared __attribute__((swift_name("shared")));
- (NSBundle * _Nullable)getBundle __attribute__((swift_name("getBundle()")));
- (BOOL)initializeBundle __attribute__((swift_name("initializeBundle()")));
@property (readonly) BOOL isInitialized __attribute__((swift_name("isInitialized")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ByteProcessing")))
@interface SharedByteProcessing : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)byteProcessing __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedByteProcessing *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t BUFFER_SIZE __attribute__((swift_name("BUFFER_SIZE")));
@property (readonly) int32_t CHUNK_SIZE __attribute__((swift_name("CHUNK_SIZE")));
@property (readonly) int32_t MAX_BUFFER_SIZE __attribute__((swift_name("MAX_BUFFER_SIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Choreography")))
@interface SharedChoreography : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)choreography __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChoreography *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t DEFAULT_DURATION __attribute__((swift_name("DEFAULT_DURATION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Midi")))
@interface SharedMidi : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)midi __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMidi *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t NOTE_DURATION_SHORT __attribute__((swift_name("NOTE_DURATION_SHORT")));
@property (readonly) int32_t VELOCITY_LOUD __attribute__((swift_name("VELOCITY_LOUD")));
@property (readonly) int32_t VELOCITY_MEDIUM __attribute__((swift_name("VELOCITY_MEDIUM")));
@property (readonly) int32_t VELOCITY_SOFT __attribute__((swift_name("VELOCITY_SOFT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MokoRes")))
@interface SharedMokoRes : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mokoRes __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMokoRes *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("ResourcesResourceContainer")))
@protocol SharedResourcesResourceContainer
@required
- (NSArray<id> *)values __attribute__((swift_name("values()")));
@property (readonly) SharedResourcesResourcePlatformDetails *__platformDetails __attribute__((swift_name("__platformDetails")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MokoRes.strings")))
@interface SharedMokoResStrings : SharedBase <SharedResourcesResourceContainer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)strings __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMokoResStrings *shared __attribute__((swift_name("shared")));
- (NSArray<SharedResourcesStringResource *> *)values __attribute__((swift_name("values()")));
@property (readonly) SharedResourcesResourcePlatformDetails *__platformDetails __attribute__((swift_name("__platformDetails")));
@property (readonly) SharedResourcesStringResource *ask_gps_enable __attribute__((swift_name("ask_gps_enable")));
@property (readonly) SharedResourcesStringResource *back __attribute__((swift_name("back")));
@property (readonly) SharedResourcesStringResource *background_description __attribute__((swift_name("background_description")));
@property (readonly) SharedResourcesStringResource *be_waved __attribute__((swift_name("be_waved")));
@property (readonly) SharedResourcesStringResource *choreography_hit __attribute__((swift_name("choreography_hit")));
@property (readonly) SharedResourcesStringResource *choreography_waiting __attribute__((swift_name("choreography_waiting")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_1 __attribute__((swift_name("choreography_warming_seq_1")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_2 __attribute__((swift_name("choreography_warming_seq_2")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_3 __attribute__((swift_name("choreography_warming_seq_3")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_4 __attribute__((swift_name("choreography_warming_seq_4")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_5 __attribute__((swift_name("choreography_warming_seq_5")));
@property (readonly) SharedResourcesStringResource *choreography_warming_seq_6 __attribute__((swift_name("choreography_warming_seq_6")));
@property (readonly) SharedResourcesStringResource *community_africa __attribute__((swift_name("community_africa")));
@property (readonly) SharedResourcesStringResource *community_asia __attribute__((swift_name("community_asia")));
@property (readonly) SharedResourcesStringResource *community_europe __attribute__((swift_name("community_europe")));
@property (readonly) SharedResourcesStringResource *community_middle_east __attribute__((swift_name("community_middle_east")));
@property (readonly) SharedResourcesStringResource *community_north_america __attribute__((swift_name("community_north_america")));
@property (readonly) SharedResourcesStringResource *community_oceania __attribute__((swift_name("community_oceania")));
@property (readonly) SharedResourcesStringResource *community_south_america __attribute__((swift_name("community_south_america")));
@property (readonly) SharedResourcesStringResource *country_argentina __attribute__((swift_name("country_argentina")));
@property (readonly) SharedResourcesStringResource *country_australia __attribute__((swift_name("country_australia")));
@property (readonly) SharedResourcesStringResource *country_brazil __attribute__((swift_name("country_brazil")));
@property (readonly) SharedResourcesStringResource *country_canada __attribute__((swift_name("country_canada")));
@property (readonly) SharedResourcesStringResource *country_chile __attribute__((swift_name("country_chile")));
@property (readonly) SharedResourcesStringResource *country_china __attribute__((swift_name("country_china")));
@property (readonly) SharedResourcesStringResource *country_colombia __attribute__((swift_name("country_colombia")));
@property (readonly) SharedResourcesStringResource *country_democratic_republic_of_the_congo __attribute__((swift_name("country_democratic_republic_of_the_congo")));
@property (readonly) SharedResourcesStringResource *country_egypt __attribute__((swift_name("country_egypt")));
@property (readonly) SharedResourcesStringResource *country_england __attribute__((swift_name("country_england")));
@property (readonly) SharedResourcesStringResource *country_france __attribute__((swift_name("country_france")));
@property (readonly) SharedResourcesStringResource *country_germany __attribute__((swift_name("country_germany")));
@property (readonly) SharedResourcesStringResource *country_india __attribute__((swift_name("country_india")));
@property (readonly) SharedResourcesStringResource *country_indonesia __attribute__((swift_name("country_indonesia")));
@property (readonly) SharedResourcesStringResource *country_iran __attribute__((swift_name("country_iran")));
@property (readonly) SharedResourcesStringResource *country_italy __attribute__((swift_name("country_italy")));
@property (readonly) SharedResourcesStringResource *country_japan __attribute__((swift_name("country_japan")));
@property (readonly) SharedResourcesStringResource *country_kenya __attribute__((swift_name("country_kenya")));
@property (readonly) SharedResourcesStringResource *country_mexico __attribute__((swift_name("country_mexico")));
@property (readonly) SharedResourcesStringResource *country_nigeria __attribute__((swift_name("country_nigeria")));
@property (readonly) SharedResourcesStringResource *country_pakistan __attribute__((swift_name("country_pakistan")));
@property (readonly) SharedResourcesStringResource *country_peru __attribute__((swift_name("country_peru")));
@property (readonly) SharedResourcesStringResource *country_philippines __attribute__((swift_name("country_philippines")));
@property (readonly) SharedResourcesStringResource *country_russia __attribute__((swift_name("country_russia")));
@property (readonly) SharedResourcesStringResource *country_south_africa __attribute__((swift_name("country_south_africa")));
@property (readonly) SharedResourcesStringResource *country_south_korea __attribute__((swift_name("country_south_korea")));
@property (readonly) SharedResourcesStringResource *country_spain __attribute__((swift_name("country_spain")));
@property (readonly) SharedResourcesStringResource *country_thailand __attribute__((swift_name("country_thailand")));
@property (readonly) SharedResourcesStringResource *country_turkey __attribute__((swift_name("country_turkey")));
@property (readonly) SharedResourcesStringResource *country_united_arab_emirates __attribute__((swift_name("country_united_arab_emirates")));
@property (readonly) SharedResourcesStringResource *country_usa __attribute__((swift_name("country_usa")));
@property (readonly) SharedResourcesStringResource *drwave __attribute__((swift_name("drwave")));
@property (readonly) SharedResourcesStringResource *empty __attribute__((swift_name("empty")));
@property (readonly) SharedResourcesStringResource *error __attribute__((swift_name("error")));
@property (readonly) SharedResourcesStringResource *event_description_bangalore_india __attribute__((swift_name("event_description_bangalore_india")));
@property (readonly) SharedResourcesStringResource *event_description_bangkok_thailand __attribute__((swift_name("event_description_bangkok_thailand")));
@property (readonly) SharedResourcesStringResource *event_description_beijing_china __attribute__((swift_name("event_description_beijing_china")));
@property (readonly) SharedResourcesStringResource *event_description_berlin_germany __attribute__((swift_name("event_description_berlin_germany")));
@property (readonly) SharedResourcesStringResource *event_description_bogota_colombia __attribute__((swift_name("event_description_bogota_colombia")));
@property (readonly) SharedResourcesStringResource *event_description_buenos_aires_argentina __attribute__((swift_name("event_description_buenos_aires_argentina")));
@property (readonly) SharedResourcesStringResource *event_description_cairo_egypt __attribute__((swift_name("event_description_cairo_egypt")));
@property (readonly) SharedResourcesStringResource *event_description_chicago_usa __attribute__((swift_name("event_description_chicago_usa")));
@property (readonly) SharedResourcesStringResource *event_description_delhi_india __attribute__((swift_name("event_description_delhi_india")));
@property (readonly) SharedResourcesStringResource *event_description_dubai_united_arab_emirates __attribute__((swift_name("event_description_dubai_united_arab_emirates")));
@property (readonly) SharedResourcesStringResource *event_description_hong_kong_china __attribute__((swift_name("event_description_hong_kong_china")));
@property (readonly) SharedResourcesStringResource *event_description_istanbul_turkey __attribute__((swift_name("event_description_istanbul_turkey")));
@property (readonly) SharedResourcesStringResource *event_description_jakarta_indonesia __attribute__((swift_name("event_description_jakarta_indonesia")));
@property (readonly) SharedResourcesStringResource *event_description_johannesburg_south_africa __attribute__((swift_name("event_description_johannesburg_south_africa")));
@property (readonly) SharedResourcesStringResource *event_description_karachi_pakistan __attribute__((swift_name("event_description_karachi_pakistan")));
@property (readonly) SharedResourcesStringResource *event_description_kinshasa_democratic_republic_of_the_congo __attribute__((swift_name("event_description_kinshasa_democratic_republic_of_the_congo")));
@property (readonly) SharedResourcesStringResource *event_description_lagos_nigeria __attribute__((swift_name("event_description_lagos_nigeria")));
@property (readonly) SharedResourcesStringResource *event_description_lima_peru __attribute__((swift_name("event_description_lima_peru")));
@property (readonly) SharedResourcesStringResource *event_description_london_england __attribute__((swift_name("event_description_london_england")));
@property (readonly) SharedResourcesStringResource *event_description_los_angeles_usa __attribute__((swift_name("event_description_los_angeles_usa")));
@property (readonly) SharedResourcesStringResource *event_description_madrid_spain __attribute__((swift_name("event_description_madrid_spain")));
@property (readonly) SharedResourcesStringResource *event_description_manila_philippines __attribute__((swift_name("event_description_manila_philippines")));
@property (readonly) SharedResourcesStringResource *event_description_melbourne_australia __attribute__((swift_name("event_description_melbourne_australia")));
@property (readonly) SharedResourcesStringResource *event_description_mexico_city_mexico __attribute__((swift_name("event_description_mexico_city_mexico")));
@property (readonly) SharedResourcesStringResource *event_description_moscow_russia __attribute__((swift_name("event_description_moscow_russia")));
@property (readonly) SharedResourcesStringResource *event_description_mumbai_india __attribute__((swift_name("event_description_mumbai_india")));
@property (readonly) SharedResourcesStringResource *event_description_nairobi_kenya __attribute__((swift_name("event_description_nairobi_kenya")));
@property (readonly) SharedResourcesStringResource *event_description_new_york_usa __attribute__((swift_name("event_description_new_york_usa")));
@property (readonly) SharedResourcesStringResource *event_description_paris_france __attribute__((swift_name("event_description_paris_france")));
@property (readonly) SharedResourcesStringResource *event_description_rome_italy __attribute__((swift_name("event_description_rome_italy")));
@property (readonly) SharedResourcesStringResource *event_description_san_francisco_usa __attribute__((swift_name("event_description_san_francisco_usa")));
@property (readonly) SharedResourcesStringResource *event_description_santiago_chile __attribute__((swift_name("event_description_santiago_chile")));
@property (readonly) SharedResourcesStringResource *event_description_sao_paulo_brazil __attribute__((swift_name("event_description_sao_paulo_brazil")));
@property (readonly) SharedResourcesStringResource *event_description_seoul_south_korea __attribute__((swift_name("event_description_seoul_south_korea")));
@property (readonly) SharedResourcesStringResource *event_description_shanghai_china __attribute__((swift_name("event_description_shanghai_china")));
@property (readonly) SharedResourcesStringResource *event_description_sydney_australia __attribute__((swift_name("event_description_sydney_australia")));
@property (readonly) SharedResourcesStringResource *event_description_tehran_iran __attribute__((swift_name("event_description_tehran_iran")));
@property (readonly) SharedResourcesStringResource *event_description_tokyo_japan __attribute__((swift_name("event_description_tokyo_japan")));
@property (readonly) SharedResourcesStringResource *event_description_toronto_canada __attribute__((swift_name("event_description_toronto_canada")));
@property (readonly) SharedResourcesStringResource *event_description_vancouver_canada __attribute__((swift_name("event_description_vancouver_canada")));
@property (readonly) SharedResourcesStringResource *event_done __attribute__((swift_name("event_done")));
@property (readonly) SharedResourcesStringResource *event_favorite_off __attribute__((swift_name("event_favorite_off")));
@property (readonly) SharedResourcesStringResource *event_favorite_on __attribute__((swift_name("event_favorite_on")));
@property (readonly) SharedResourcesStringResource *event_location_bangalore_india __attribute__((swift_name("event_location_bangalore_india")));
@property (readonly) SharedResourcesStringResource *event_location_bangkok_thailand __attribute__((swift_name("event_location_bangkok_thailand")));
@property (readonly) SharedResourcesStringResource *event_location_beijing_china __attribute__((swift_name("event_location_beijing_china")));
@property (readonly) SharedResourcesStringResource *event_location_berlin_germany __attribute__((swift_name("event_location_berlin_germany")));
@property (readonly) SharedResourcesStringResource *event_location_bogota_colombia __attribute__((swift_name("event_location_bogota_colombia")));
@property (readonly) SharedResourcesStringResource *event_location_buenos_aires_argentina __attribute__((swift_name("event_location_buenos_aires_argentina")));
@property (readonly) SharedResourcesStringResource *event_location_cairo_egypt __attribute__((swift_name("event_location_cairo_egypt")));
@property (readonly) SharedResourcesStringResource *event_location_chicago_usa __attribute__((swift_name("event_location_chicago_usa")));
@property (readonly) SharedResourcesStringResource *event_location_delhi_india __attribute__((swift_name("event_location_delhi_india")));
@property (readonly) SharedResourcesStringResource *event_location_dubai_united_arab_emirates __attribute__((swift_name("event_location_dubai_united_arab_emirates")));
@property (readonly) SharedResourcesStringResource *event_location_hong_kong_china __attribute__((swift_name("event_location_hong_kong_china")));
@property (readonly) SharedResourcesStringResource *event_location_istanbul_turkey __attribute__((swift_name("event_location_istanbul_turkey")));
@property (readonly) SharedResourcesStringResource *event_location_jakarta_indonesia __attribute__((swift_name("event_location_jakarta_indonesia")));
@property (readonly) SharedResourcesStringResource *event_location_johannesburg_south_africa __attribute__((swift_name("event_location_johannesburg_south_africa")));
@property (readonly) SharedResourcesStringResource *event_location_karachi_pakistan __attribute__((swift_name("event_location_karachi_pakistan")));
@property (readonly) SharedResourcesStringResource *event_location_kinshasa_democratic_republic_of_the_congo __attribute__((swift_name("event_location_kinshasa_democratic_republic_of_the_congo")));
@property (readonly) SharedResourcesStringResource *event_location_lagos_nigeria __attribute__((swift_name("event_location_lagos_nigeria")));
@property (readonly) SharedResourcesStringResource *event_location_lima_peru __attribute__((swift_name("event_location_lima_peru")));
@property (readonly) SharedResourcesStringResource *event_location_london_england __attribute__((swift_name("event_location_london_england")));
@property (readonly) SharedResourcesStringResource *event_location_los_angeles_usa __attribute__((swift_name("event_location_los_angeles_usa")));
@property (readonly) SharedResourcesStringResource *event_location_madrid_spain __attribute__((swift_name("event_location_madrid_spain")));
@property (readonly) SharedResourcesStringResource *event_location_manila_philippines __attribute__((swift_name("event_location_manila_philippines")));
@property (readonly) SharedResourcesStringResource *event_location_melbourne_australia __attribute__((swift_name("event_location_melbourne_australia")));
@property (readonly) SharedResourcesStringResource *event_location_mexico_city_mexico __attribute__((swift_name("event_location_mexico_city_mexico")));
@property (readonly) SharedResourcesStringResource *event_location_moscow_russia __attribute__((swift_name("event_location_moscow_russia")));
@property (readonly) SharedResourcesStringResource *event_location_mumbai_india __attribute__((swift_name("event_location_mumbai_india")));
@property (readonly) SharedResourcesStringResource *event_location_nairobi_kenya __attribute__((swift_name("event_location_nairobi_kenya")));
@property (readonly) SharedResourcesStringResource *event_location_new_york_usa __attribute__((swift_name("event_location_new_york_usa")));
@property (readonly) SharedResourcesStringResource *event_location_paris_france __attribute__((swift_name("event_location_paris_france")));
@property (readonly) SharedResourcesStringResource *event_location_rome_italy __attribute__((swift_name("event_location_rome_italy")));
@property (readonly) SharedResourcesStringResource *event_location_san_francisco_usa __attribute__((swift_name("event_location_san_francisco_usa")));
@property (readonly) SharedResourcesStringResource *event_location_santiago_chile __attribute__((swift_name("event_location_santiago_chile")));
@property (readonly) SharedResourcesStringResource *event_location_sao_paulo_brazil __attribute__((swift_name("event_location_sao_paulo_brazil")));
@property (readonly) SharedResourcesStringResource *event_location_seoul_south_korea __attribute__((swift_name("event_location_seoul_south_korea")));
@property (readonly) SharedResourcesStringResource *event_location_shanghai_china __attribute__((swift_name("event_location_shanghai_china")));
@property (readonly) SharedResourcesStringResource *event_location_sydney_australia __attribute__((swift_name("event_location_sydney_australia")));
@property (readonly) SharedResourcesStringResource *event_location_tehran_iran __attribute__((swift_name("event_location_tehran_iran")));
@property (readonly) SharedResourcesStringResource *event_location_tokyo_japan __attribute__((swift_name("event_location_tokyo_japan")));
@property (readonly) SharedResourcesStringResource *event_location_toronto_canada __attribute__((swift_name("event_location_toronto_canada")));
@property (readonly) SharedResourcesStringResource *event_location_vancouver_canada __attribute__((swift_name("event_location_vancouver_canada")));
@property (readonly) SharedResourcesStringResource *event_running __attribute__((swift_name("event_running")));
@property (readonly) SharedResourcesStringResource *event_soon __attribute__((swift_name("event_soon")));
@property (readonly) SharedResourcesStringResource *event_target_me_off __attribute__((swift_name("event_target_me_off")));
@property (readonly) SharedResourcesStringResource *event_target_me_on __attribute__((swift_name("event_target_me_on")));
@property (readonly) SharedResourcesStringResource *event_target_wave_off __attribute__((swift_name("event_target_wave_off")));
@property (readonly) SharedResourcesStringResource *event_target_wave_on __attribute__((swift_name("event_target_wave_on")));
@property (readonly) SharedResourcesStringResource *events_cannot_uninstall_map_message __attribute__((swift_name("events_cannot_uninstall_map_message")));
@property (readonly) SharedResourcesStringResource *events_downloaded_empty __attribute__((swift_name("events_downloaded_empty")));
@property (readonly) SharedResourcesStringResource *events_empty __attribute__((swift_name("events_empty")));
@property (readonly) SharedResourcesStringResource *events_favorites_empty __attribute__((swift_name("events_favorites_empty")));
@property (readonly) SharedResourcesStringResource *events_loading_error __attribute__((swift_name("events_loading_error")));
@property (readonly) SharedResourcesStringResource *events_not_found_loading __attribute__((swift_name("events_not_found_loading")));
@property (readonly) SharedResourcesStringResource *events_select_all __attribute__((swift_name("events_select_all")));
@property (readonly) SharedResourcesStringResource *events_select_downloaded __attribute__((swift_name("events_select_downloaded")));
@property (readonly) SharedResourcesStringResource *events_select_starred __attribute__((swift_name("events_select_starred")));
@property (readonly) SharedResourcesStringResource *events_uninstall __attribute__((swift_name("events_uninstall")));
@property (readonly) SharedResourcesStringResource *events_uninstall_cancel __attribute__((swift_name("events_uninstall_cancel")));
@property (readonly) SharedResourcesStringResource *events_uninstall_completed __attribute__((swift_name("events_uninstall_completed")));
@property (readonly) SharedResourcesStringResource *events_uninstall_failed __attribute__((swift_name("events_uninstall_failed")));
@property (readonly) SharedResourcesStringResource *events_uninstall_map_confirmation __attribute__((swift_name("events_uninstall_map_confirmation")));
@property (readonly) SharedResourcesStringResource *events_uninstall_map_title __attribute__((swift_name("events_uninstall_map_title")));
@property (readonly) SharedResourcesStringResource *faq __attribute__((swift_name("faq")));
@property (readonly) SharedResourcesStringResource *faq_access __attribute__((swift_name("faq_access")));
@property (readonly) SharedResourcesStringResource *faq_answer_1 __attribute__((swift_name("faq_answer_1")));
@property (readonly) SharedResourcesStringResource *faq_answer_2 __attribute__((swift_name("faq_answer_2")));
@property (readonly) SharedResourcesStringResource *faq_answer_3 __attribute__((swift_name("faq_answer_3")));
@property (readonly) SharedResourcesStringResource *faq_answer_4 __attribute__((swift_name("faq_answer_4")));
@property (readonly) SharedResourcesStringResource *faq_answer_5 __attribute__((swift_name("faq_answer_5")));
@property (readonly) SharedResourcesStringResource *faq_answer_6 __attribute__((swift_name("faq_answer_6")));
@property (readonly) SharedResourcesStringResource *faq_question_1 __attribute__((swift_name("faq_question_1")));
@property (readonly) SharedResourcesStringResource *faq_question_2 __attribute__((swift_name("faq_question_2")));
@property (readonly) SharedResourcesStringResource *faq_question_3 __attribute__((swift_name("faq_question_3")));
@property (readonly) SharedResourcesStringResource *faq_question_4 __attribute__((swift_name("faq_question_4")));
@property (readonly) SharedResourcesStringResource *faq_question_5 __attribute__((swift_name("faq_question_5")));
@property (readonly) SharedResourcesStringResource *faq_question_6 __attribute__((swift_name("faq_question_6")));
@property (readonly) SharedResourcesStringResource *geoloc_error __attribute__((swift_name("geoloc_error")));
@property (readonly) SharedResourcesStringResource *geoloc_undone __attribute__((swift_name("geoloc_undone")));
@property (readonly) SharedResourcesStringResource *geoloc_warm_in __attribute__((swift_name("geoloc_warm_in")));
@property (readonly) SharedResourcesStringResource *geoloc_yourein __attribute__((swift_name("geoloc_yourein")));
@property (readonly) SharedResourcesStringResource *geoloc_yourein_at __attribute__((swift_name("geoloc_yourein_at")));
@property (readonly) SharedResourcesStringResource *geoloc_yourenotin __attribute__((swift_name("geoloc_yourenotin")));
@property (readonly) SharedResourcesStringResource *hour_plural __attribute__((swift_name("hour_plural")));
@property (readonly) SharedResourcesStringResource *hour_singular __attribute__((swift_name("hour_singular")));
@property (readonly) SharedResourcesStringResource *infos_core_1 __attribute__((swift_name("infos_core_1")));
@property (readonly) SharedResourcesStringResource *infos_core_2 __attribute__((swift_name("infos_core_2")));
@property (readonly) SharedResourcesStringResource *infos_core_3 __attribute__((swift_name("infos_core_3")));
@property (readonly) SharedResourcesStringResource *infos_core_4 __attribute__((swift_name("infos_core_4")));
@property (readonly) SharedResourcesStringResource *infos_core_5 __attribute__((swift_name("infos_core_5")));
@property (readonly) SharedResourcesStringResource *infos_core_6 __attribute__((swift_name("infos_core_6")));
@property (readonly) SharedResourcesStringResource *infos_core_7 __attribute__((swift_name("infos_core_7")));
@property (readonly) SharedResourcesStringResource *infos_core_8 __attribute__((swift_name("infos_core_8")));
@property (readonly) SharedResourcesStringResource *infos_core_9 __attribute__((swift_name("infos_core_9")));
@property (readonly) SharedResourcesStringResource *instagram_logo_description __attribute__((swift_name("instagram_logo_description")));
@property (readonly) SharedResourcesStringResource *logo_description __attribute__((swift_name("logo_description")));
@property (readonly) SharedResourcesStringResource *map_cancel_download __attribute__((swift_name("map_cancel_download")));
@property (readonly) SharedResourcesStringResource *map_checking_state __attribute__((swift_name("map_checking_state")));
@property (readonly) SharedResourcesStringResource *map_download __attribute__((swift_name("map_download")));
@property (readonly) SharedResourcesStringResource *map_downloaded __attribute__((swift_name("map_downloaded")));
@property (readonly) SharedResourcesStringResource *map_downloading __attribute__((swift_name("map_downloading")));
@property (readonly) SharedResourcesStringResource *map_error_access_denied __attribute__((swift_name("map_error_access_denied")));
@property (readonly) SharedResourcesStringResource *map_error_account_issue __attribute__((swift_name("map_error_account_issue")));
@property (readonly) SharedResourcesStringResource *map_error_active_sessions_limit __attribute__((swift_name("map_error_active_sessions_limit")));
@property (readonly) SharedResourcesStringResource *map_error_api_not_available __attribute__((swift_name("map_error_api_not_available")));
@property (readonly) SharedResourcesStringResource *map_error_download __attribute__((swift_name("map_error_download")));
@property (readonly) SharedResourcesStringResource *map_error_failed_after_retries __attribute__((swift_name("map_error_failed_after_retries")));
@property (readonly) SharedResourcesStringResource *map_error_incompatible_with_existing_session __attribute__((swift_name("map_error_incompatible_with_existing_session")));
@property (readonly) SharedResourcesStringResource *map_error_insufficient_storage __attribute__((swift_name("map_error_insufficient_storage")));
@property (readonly) SharedResourcesStringResource *map_error_invalid_request __attribute__((swift_name("map_error_invalid_request")));
@property (readonly) SharedResourcesStringResource *map_error_module_unavailable __attribute__((swift_name("map_error_module_unavailable")));
@property (readonly) SharedResourcesStringResource *map_error_network __attribute__((swift_name("map_error_network")));
@property (readonly) SharedResourcesStringResource *map_error_service_died __attribute__((swift_name("map_error_service_died")));
@property (readonly) SharedResourcesStringResource *map_error_unknown __attribute__((swift_name("map_error_unknown")));
@property (readonly) SharedResourcesStringResource *map_installing __attribute__((swift_name("map_installing")));
@property (readonly) SharedResourcesStringResource *map_loading __attribute__((swift_name("map_loading")));
@property (readonly) SharedResourcesStringResource *map_retry_download __attribute__((swift_name("map_retry_download")));
@property (readonly) SharedResourcesStringResource *map_retrying_download __attribute__((swift_name("map_retrying_download")));
@property (readonly) SharedResourcesStringResource *map_starting_download __attribute__((swift_name("map_starting_download")));
@property (readonly) SharedResourcesStringResource *minute_plural __attribute__((swift_name("minute_plural")));
@property (readonly) SharedResourcesStringResource *minute_singular __attribute__((swift_name("minute_singular")));
@property (readonly) SharedResourcesStringResource *no __attribute__((swift_name("no")));
@property (readonly) SharedResourcesStringResource *ok __attribute__((swift_name("ok")));
@property (readonly) SharedResourcesStringResource *simulation_map_required_message __attribute__((swift_name("simulation_map_required_message")));
@property (readonly) SharedResourcesStringResource *simulation_map_required_title __attribute__((swift_name("simulation_map_required_title")));
@property (readonly) SharedResourcesStringResource *simulation_mode __attribute__((swift_name("simulation_mode")));
@property (readonly) SharedResourcesStringResource *simulation_stop __attribute__((swift_name("simulation_stop")));
@property (readonly) SharedResourcesStringResource *speed_unit_mps __attribute__((swift_name("speed_unit_mps")));
@property (readonly) SharedResourcesStringResource *tab_faq_name __attribute__((swift_name("tab_faq_name")));
@property (readonly) SharedResourcesStringResource *tab_infos_name __attribute__((swift_name("tab_infos_name")));
@property (readonly) SharedResourcesStringResource *test_simulation __attribute__((swift_name("test_simulation")));
@property (readonly) SharedResourcesStringResource *test_simulation_started __attribute__((swift_name("test_simulation_started")));
@property (readonly) SharedResourcesStringResource *warn_emergency_item_1 __attribute__((swift_name("warn_emergency_item_1")));
@property (readonly) SharedResourcesStringResource *warn_emergency_item_2 __attribute__((swift_name("warn_emergency_item_2")));
@property (readonly) SharedResourcesStringResource *warn_emergency_item_3 __attribute__((swift_name("warn_emergency_item_3")));
@property (readonly) SharedResourcesStringResource *warn_emergency_title __attribute__((swift_name("warn_emergency_title")));
@property (readonly) SharedResourcesStringResource *warn_general_item_1 __attribute__((swift_name("warn_general_item_1")));
@property (readonly) SharedResourcesStringResource *warn_general_item_2 __attribute__((swift_name("warn_general_item_2")));
@property (readonly) SharedResourcesStringResource *warn_general_item_3 __attribute__((swift_name("warn_general_item_3")));
@property (readonly) SharedResourcesStringResource *warn_general_item_4 __attribute__((swift_name("warn_general_item_4")));
@property (readonly) SharedResourcesStringResource *warn_general_item_5 __attribute__((swift_name("warn_general_item_5")));
@property (readonly) SharedResourcesStringResource *warn_general_item_6 __attribute__((swift_name("warn_general_item_6")));
@property (readonly) SharedResourcesStringResource *warn_general_title __attribute__((swift_name("warn_general_title")));
@property (readonly) SharedResourcesStringResource *warn_legal_item_1 __attribute__((swift_name("warn_legal_item_1")));
@property (readonly) SharedResourcesStringResource *warn_legal_item_2 __attribute__((swift_name("warn_legal_item_2")));
@property (readonly) SharedResourcesStringResource *warn_legal_title __attribute__((swift_name("warn_legal_title")));
@property (readonly) SharedResourcesStringResource *warn_rules_security_text __attribute__((swift_name("warn_rules_security_text")));
@property (readonly) SharedResourcesStringResource *warn_rules_security_title __attribute__((swift_name("warn_rules_security_title")));
@property (readonly) SharedResourcesStringResource *warn_safety_item_1 __attribute__((swift_name("warn_safety_item_1")));
@property (readonly) SharedResourcesStringResource *warn_safety_item_2 __attribute__((swift_name("warn_safety_item_2")));
@property (readonly) SharedResourcesStringResource *warn_safety_item_3 __attribute__((swift_name("warn_safety_item_3")));
@property (readonly) SharedResourcesStringResource *warn_safety_item_4 __attribute__((swift_name("warn_safety_item_4")));
@property (readonly) SharedResourcesStringResource *warn_safety_item_5 __attribute__((swift_name("warn_safety_item_5")));
@property (readonly) SharedResourcesStringResource *warn_safety_title __attribute__((swift_name("warn_safety_title")));
@property (readonly) SharedResourcesStringResource *wave_be_ready __attribute__((swift_name("wave_be_ready")));
@property (readonly) SharedResourcesStringResource *wave_done __attribute__((swift_name("wave_done")));
@property (readonly) SharedResourcesStringResource *wave_end_time __attribute__((swift_name("wave_end_time")));
@property (readonly) SharedResourcesStringResource *wave_hit __attribute__((swift_name("wave_hit")));
@property (readonly) SharedResourcesStringResource *wave_is_running __attribute__((swift_name("wave_is_running")));
@property (readonly) SharedResourcesStringResource *wave_now __attribute__((swift_name("wave_now")));
@property (readonly) SharedResourcesStringResource *wave_progression __attribute__((swift_name("wave_progression")));
@property (readonly) SharedResourcesStringResource *wave_speed __attribute__((swift_name("wave_speed")));
@property (readonly) SharedResourcesStringResource *wave_start_time __attribute__((swift_name("wave_start_time")));
@property (readonly) SharedResourcesStringResource *wave_total_time __attribute__((swift_name("wave_total_time")));
@property (readonly) SharedResourcesStringResource *wave_warming __attribute__((swift_name("wave_warming")));
@property (readonly) SharedResourcesStringResource *www_hashtag __attribute__((swift_name("www_hashtag")));
@property (readonly) SharedResourcesStringResource *www_instagram __attribute__((swift_name("www_instagram")));
@property (readonly) SharedResourcesStringResource *www_instagram_url __attribute__((swift_name("www_instagram_url")));
@property (readonly) SharedResourcesStringResource *yes __attribute__((swift_name("yes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals")))
@interface SharedWWWGlobals : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)wWWGlobals __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobals *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Audio")))
@interface SharedWWWGlobalsAudio : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)audio __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsAudio *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DEFAULT_BITS_PER_SAMPLE __attribute__((swift_name("DEFAULT_BITS_PER_SAMPLE")));
@property (readonly) int32_t DEFAULT_CHANNELS __attribute__((swift_name("DEFAULT_CHANNELS")));
@property (readonly) double ENVELOPE_ATTACK_TIME __attribute__((swift_name("ENVELOPE_ATTACK_TIME")));
@property (readonly) double ENVELOPE_RELEASE_TIME __attribute__((swift_name("ENVELOPE_RELEASE_TIME")));
@property (readonly) int32_t STANDARD_SAMPLE_RATE __attribute__((swift_name("STANDARD_SAMPLE_RATE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.BackNav")))
@interface SharedWWWGlobalsBackNav : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)backNav __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsBackNav *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t EVENT_LOCATION_FONTSIZE __attribute__((swift_name("EVENT_LOCATION_FONTSIZE")));
@property (readonly) int32_t FONTSIZE __attribute__((swift_name("FONTSIZE")));
@property (readonly) NSArray<SharedInt *> *PADDING __attribute__((swift_name("PADDING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.ByteProcessing")))
@interface SharedWWWGlobalsByteProcessing : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)byteProcessing __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsByteProcessing *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t AUDIO_16BIT_MAX __attribute__((swift_name("AUDIO_16BIT_MAX")));
@property (readonly) int32_t AUDIO_16BIT_MIN __attribute__((swift_name("AUDIO_16BIT_MIN")));
@property (readonly) int32_t AUDIO_8BIT_MAX __attribute__((swift_name("AUDIO_8BIT_MAX")));
@property (readonly) double AUDIO_8BIT_SCALE __attribute__((swift_name("AUDIO_8BIT_SCALE")));
@property (readonly) int32_t BIT_SHIFT_16 __attribute__((swift_name("BIT_SHIFT_16")));
@property (readonly) int32_t BIT_SHIFT_24 __attribute__((swift_name("BIT_SHIFT_24")));
@property (readonly) int32_t BIT_SHIFT_8 __attribute__((swift_name("BIT_SHIFT_8")));
@property (readonly) int32_t BUFFER_SIZE __attribute__((swift_name("BUFFER_SIZE")));
@property (readonly) int32_t BYTES_PER_16BIT_SAMPLE __attribute__((swift_name("BYTES_PER_16BIT_SAMPLE")));
@property (readonly) int32_t BYTE_MASK __attribute__((swift_name("BYTE_MASK")));
@property (readonly) int32_t VLQ_BIT_SHIFT __attribute__((swift_name("VLQ_BIT_SHIFT")));
@property (readonly) int32_t VLQ_CONTINUATION_MASK __attribute__((swift_name("VLQ_CONTINUATION_MASK")));
@property (readonly) int32_t VLQ_DATA_MASK __attribute__((swift_name("VLQ_DATA_MASK")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Choreography")))
@interface SharedWWWGlobalsChoreography : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)choreography __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsChoreography *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t FIRST_WARMING_SEQ __attribute__((swift_name("FIRST_WARMING_SEQ")));
@property (readonly) int32_t MAX_WARMING_SEQUENCES __attribute__((swift_name("MAX_WARMING_SEQUENCES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Common")))
@interface SharedWWWGlobalsCommon : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)common __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsCommon *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DONE_IMAGE_WIDTH __attribute__((swift_name("DONE_IMAGE_WIDTH")));
@property (readonly) int32_t SOCIALNETWORKS_ACCOUNT_FONTSIZE __attribute__((swift_name("SOCIALNETWORKS_ACCOUNT_FONTSIZE")));
@property (readonly) int32_t SOCIALNETWORKS_HASHTAG_FONTSIZE __attribute__((swift_name("SOCIALNETWORKS_HASHTAG_FONTSIZE")));
@property (readonly) int32_t SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH __attribute__((swift_name("SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH")));
@property (readonly) int32_t SOONRUNNING_FONTSIZE __attribute__((swift_name("SOONRUNNING_FONTSIZE")));
@property (readonly) int32_t SOONRUNNING_HEIGHT __attribute__((swift_name("SOONRUNNING_HEIGHT")));
@property (readonly) int32_t SOONRUNNING_PADDING __attribute__((swift_name("SOONRUNNING_PADDING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Dimensions")))
@interface SharedWWWGlobalsDimensions : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dimensions __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsDimensions *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DEFAULT_EXT_PADDING __attribute__((swift_name("DEFAULT_EXT_PADDING")));
@property (readonly) int32_t DEFAULT_INT_PADDING __attribute__((swift_name("DEFAULT_INT_PADDING")));
@property (readonly) int32_t DIVIDER_THICKNESS __attribute__((swift_name("DIVIDER_THICKNESS")));
@property (readonly) int32_t DIVIDER_WIDTH __attribute__((swift_name("DIVIDER_WIDTH")));
@property (readonly) int32_t FONTSIZE_BIG __attribute__((swift_name("FONTSIZE_BIG")));
@property (readonly) int32_t FONTSIZE_BIG2 __attribute__((swift_name("FONTSIZE_BIG2")));
@property (readonly) int32_t FONTSIZE_BIG3 __attribute__((swift_name("FONTSIZE_BIG3")));
@property (readonly) int32_t FONTSIZE_BIG4 __attribute__((swift_name("FONTSIZE_BIG4")));
@property (readonly) int32_t FONTSIZE_DEFAULT __attribute__((swift_name("FONTSIZE_DEFAULT")));
@property (readonly) int32_t FONTSIZE_HUGE __attribute__((swift_name("FONTSIZE_HUGE")));
@property (readonly) int32_t FONTSIZE_HUGE2 __attribute__((swift_name("FONTSIZE_HUGE2")));
@property (readonly) int32_t FONTSIZE_MEDIUM __attribute__((swift_name("FONTSIZE_MEDIUM")));
@property (readonly) int32_t FONTSIZE_MEDIUM2 __attribute__((swift_name("FONTSIZE_MEDIUM2")));
@property (readonly) int32_t FONTSIZE_SMALL __attribute__((swift_name("FONTSIZE_SMALL")));
@property (readonly) int32_t FONTSIZE_SMALL2 __attribute__((swift_name("FONTSIZE_SMALL2")));
@property (readonly) int32_t SPACER_BIG __attribute__((swift_name("SPACER_BIG")));
@property (readonly) int32_t SPACER_MEDIUM __attribute__((swift_name("SPACER_MEDIUM")));
@property (readonly) int32_t SPACER_SMALL __attribute__((swift_name("SPACER_SMALL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.DisplayText")))
@interface SharedWWWGlobalsDisplayText : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)displayText __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsDisplayText *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *EMPTY_COUNTER __attribute__((swift_name("EMPTY_COUNTER")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Event")))
@interface SharedWWWGlobalsEvent : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)event __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsEvent *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DATE_FONTSIZE __attribute__((swift_name("DATE_FONTSIZE")));
@property (readonly) float DATE_MITER __attribute__((swift_name("DATE_MITER")));
@property (readonly) float DATE_STROKE __attribute__((swift_name("DATE_STROKE")));
@property (readonly) int32_t DESC_FONTSIZE __attribute__((swift_name("DESC_FONTSIZE")));
@property (readonly) int32_t GEOLOCME_BORDER __attribute__((swift_name("GEOLOCME_BORDER")));
@property (readonly) int32_t GEOLOCME_FONTSIZE __attribute__((swift_name("GEOLOCME_FONTSIZE")));
@property (readonly) int32_t GEOLOCME_HEIGHT __attribute__((swift_name("GEOLOCME_HEIGHT")));
@property (readonly) int32_t GEOLOC_FONTSIZE __attribute__((swift_name("GEOLOC_FONTSIZE")));
@property (readonly) float MAP_RATIO __attribute__((swift_name("MAP_RATIO")));
@property (readonly) int32_t NUMBERS_BORDERROUND __attribute__((swift_name("NUMBERS_BORDERROUND")));
@property (readonly) int32_t NUMBERS_BORDERWIDTH __attribute__((swift_name("NUMBERS_BORDERWIDTH")));
@property (readonly) int32_t NUMBERS_FONTSIZE __attribute__((swift_name("NUMBERS_FONTSIZE")));
@property (readonly) int32_t NUMBERS_LABEL_FONTSIZE __attribute__((swift_name("NUMBERS_LABEL_FONTSIZE")));
@property (readonly) int32_t NUMBERS_SPACER __attribute__((swift_name("NUMBERS_SPACER")));
@property (readonly) int32_t NUMBERS_TITLE_FONTSIZE __attribute__((swift_name("NUMBERS_TITLE_FONTSIZE")));
@property (readonly) int32_t NUMBERS_TZ_FONTSIZE __attribute__((swift_name("NUMBERS_TZ_FONTSIZE")));
@property (readonly) int32_t NUMBERS_VALUE_FONTSIZE __attribute__((swift_name("NUMBERS_VALUE_FONTSIZE")));
@property (readonly) int32_t TARGET_ME_IMAGE_SIZE __attribute__((swift_name("TARGET_ME_IMAGE_SIZE")));
@property (readonly) int32_t TARGET_WAVE_IMAGE_SIZE __attribute__((swift_name("TARGET_WAVE_IMAGE_SIZE")));
@property (readonly) int32_t WAVEBUTTON_FONTSIZE __attribute__((swift_name("WAVEBUTTON_FONTSIZE")));
@property (readonly) int32_t WAVEBUTTON_HEIGHT __attribute__((swift_name("WAVEBUTTON_HEIGHT")));
@property (readonly) int32_t WAVEBUTTON_WIDTH __attribute__((swift_name("WAVEBUTTON_WIDTH")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.EventsList")))
@interface SharedWWWGlobalsEventsList : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eventsList __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsEventsList *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t EVENT_COMMUNITY_FONTSIZE __attribute__((swift_name("EVENT_COMMUNITY_FONTSIZE")));
@property (readonly) int32_t EVENT_COUNTRY_FONTSIZE __attribute__((swift_name("EVENT_COUNTRY_FONTSIZE")));
@property (readonly) int32_t EVENT_DATE_FONTSIZE __attribute__((swift_name("EVENT_DATE_FONTSIZE")));
@property (readonly) int32_t EVENT_LOCATION_FONTSIZE __attribute__((swift_name("EVENT_LOCATION_FONTSIZE")));
@property (readonly) int32_t FAVS_IMAGE_SIZE __attribute__((swift_name("FAVS_IMAGE_SIZE")));
@property (readonly) int32_t FLAG_WIDTH __attribute__((swift_name("FLAG_WIDTH")));
@property (readonly) int32_t MAPDL_IMAGE_SIZE __attribute__((swift_name("MAPDL_IMAGE_SIZE")));
@property (readonly) int32_t NOEVENTS_FONTSIZE __attribute__((swift_name("NOEVENTS_FONTSIZE")));
@property (readonly) int32_t OVERLAY_HEIGHT __attribute__((swift_name("OVERLAY_HEIGHT")));
@property (readonly) int32_t SELECTOR_FONTSIZE __attribute__((swift_name("SELECTOR_FONTSIZE")));
@property (readonly) int32_t SELECTOR_HEIGHT __attribute__((swift_name("SELECTOR_HEIGHT")));
@property (readonly) int32_t SELECTOR_ROUND __attribute__((swift_name("SELECTOR_ROUND")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.FAQ")))
@interface SharedWWWGlobalsFAQ : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fAQ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsFAQ *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t ACCESS_LINK_FONTSIZE __attribute__((swift_name("ACCESS_LINK_FONTSIZE")));
@property (readonly) int32_t ANSWER_FONTSIZE __attribute__((swift_name("ANSWER_FONTSIZE")));
@property (readonly) int32_t INTRO_FONTSIZE __attribute__((swift_name("INTRO_FONTSIZE")));
@property (readonly) int32_t LINK_FONTSIZE __attribute__((swift_name("LINK_FONTSIZE")));
@property (readonly) int32_t QUESTION_FONTSIZE __attribute__((swift_name("QUESTION_FONTSIZE")));
@property (readonly) int32_t RULES_TEXT_FONTSIZE __attribute__((swift_name("RULES_TEXT_FONTSIZE")));
@property (readonly) int32_t RULES_TITLE_FONTSIZE __attribute__((swift_name("RULES_TITLE_FONTSIZE")));
@property (readonly) int32_t RULE_ANSWER_FONTSIZE __attribute__((swift_name("RULE_ANSWER_FONTSIZE")));
@property (readonly) int32_t RULE_CONTENTS_FONTSIZE __attribute__((swift_name("RULE_CONTENTS_FONTSIZE")));
@property (readonly) int32_t RULE_NBRING_WIDTH __attribute__((swift_name("RULE_NBRING_WIDTH")));
@property (readonly) int32_t RULE_QUESTION_FONTSIZE __attribute__((swift_name("RULE_QUESTION_FONTSIZE")));
@property (readonly) int32_t RULE_TITLE_FONTSIZE __attribute__((swift_name("RULE_TITLE_FONTSIZE")));
@property (readonly) int32_t SECTION_TITLE_FONTSIZE __attribute__((swift_name("SECTION_TITLE_FONTSIZE")));
@property (readonly) int32_t SIMULATE_BUTTON_FONTSIZE __attribute__((swift_name("SIMULATE_BUTTON_FONTSIZE")));
@property (readonly) int32_t TEXT_FONTSIZE __attribute__((swift_name("TEXT_FONTSIZE")));
@property (readonly) int32_t TITLE_FONTSIZE __attribute__((swift_name("TITLE_FONTSIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.FileSystem")))
@interface SharedWWWGlobalsFileSystem : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fileSystem __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsFileSystem *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *CHOREOGRAPHIES_CONF __attribute__((swift_name("CHOREOGRAPHIES_CONF")));
@property (readonly) NSString *CHOREOGRAPHIES_SOUND_MIDIFILE __attribute__((swift_name("CHOREOGRAPHIES_SOUND_MIDIFILE")));
@property (readonly) NSString *DATASTORE_FOLDER __attribute__((swift_name("DATASTORE_FOLDER")));
@property (readonly) NSString *EVENTS_CONF __attribute__((swift_name("EVENTS_CONF")));
@property (readonly) NSString *MAPS_STYLE __attribute__((swift_name("MAPS_STYLE")));
@property (readonly) NSString *STYLE_FOLDER __attribute__((swift_name("STYLE_FOLDER")));
@property (readonly) NSString *STYLE_LISTING __attribute__((swift_name("STYLE_LISTING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Geodetic")))
@interface SharedWWWGlobalsGeodetic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)geodetic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsGeodetic *shared __attribute__((swift_name("shared")));
@property (readonly) double COORDINATE_EPSILON __attribute__((swift_name("COORDINATE_EPSILON")));
@property (readonly) double EARTH_RADIUS __attribute__((swift_name("EARTH_RADIUS")));
@property (readonly) double HALF_PLANE_TOLERANCE __attribute__((swift_name("HALF_PLANE_TOLERANCE")));
@property (readonly) double MIN_PERCEPTIBLE_SPEED_DIFFERENCE __attribute__((swift_name("MIN_PERCEPTIBLE_SPEED_DIFFERENCE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Info")))
@interface SharedWWWGlobalsInfo : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)info __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsInfo *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DRWAVE_FONTSIZE __attribute__((swift_name("DRWAVE_FONTSIZE")));
@property (readonly) int32_t TEXT_FONTSIZE __attribute__((swift_name("TEXT_FONTSIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.LocationAccuracy")))
@interface SharedWWWGlobalsLocationAccuracy : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)locationAccuracy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsLocationAccuracy *shared __attribute__((swift_name("shared")));
@property (readonly) float GPS_HIGH_ACCURACY_THRESHOLD __attribute__((swift_name("GPS_HIGH_ACCURACY_THRESHOLD")));
@property (readonly) float GPS_LOW_ACCURACY_THRESHOLD __attribute__((swift_name("GPS_LOW_ACCURACY_THRESHOLD")));
@property (readonly) float GPS_MEDIUM_ACCURACY_THRESHOLD __attribute__((swift_name("GPS_MEDIUM_ACCURACY_THRESHOLD")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.MapDisplay")))
@interface SharedWWWGlobalsMapDisplay : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mapDisplay __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsMapDisplay *shared __attribute__((swift_name("shared")));
@property (readonly) double CHANGE_THRESHOLD __attribute__((swift_name("CHANGE_THRESHOLD")));
@property (readonly) double CONSTRAINT_EXTRA_MARGIN __attribute__((swift_name("CONSTRAINT_EXTRA_MARGIN")));
@property (readonly) double CONSTRAINT_LARGE_THRESHOLD __attribute__((swift_name("CONSTRAINT_LARGE_THRESHOLD")));
@property (readonly) double CONSTRAINT_MEDIUM_THRESHOLD __attribute__((swift_name("CONSTRAINT_MEDIUM_THRESHOLD")));
@property (readonly) double CONSTRAINT_PADDING_MULTIPLIER __attribute__((swift_name("CONSTRAINT_PADDING_MULTIPLIER")));
@property (readonly) double CONSTRAINT_SMALL_THRESHOLD __attribute__((swift_name("CONSTRAINT_SMALL_THRESHOLD")));
@property (readonly) double DEGREES_TO_RADIANS_FACTOR __attribute__((swift_name("DEGREES_TO_RADIANS_FACTOR")));
@property (readonly) double TARGET_USER_ZOOM __attribute__((swift_name("TARGET_USER_ZOOM")));
@property (readonly) double TARGET_WAVE_ZOOM __attribute__((swift_name("TARGET_WAVE_ZOOM")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Midi")))
@interface SharedWWWGlobalsMidi : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)midi __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsMidi *shared __attribute__((swift_name("shared")));
@property (readonly) double A4_FREQUENCY __attribute__((swift_name("A4_FREQUENCY")));
@property (readonly) int32_t A4_MIDI_NOTE __attribute__((swift_name("A4_MIDI_NOTE")));
@property (readonly) int64_t DEFAULT_MICROSECONDS_PER_BEAT __attribute__((swift_name("DEFAULT_MICROSECONDS_PER_BEAT")));
@property (readonly) int32_t DEFAULT_OCTAVE __attribute__((swift_name("DEFAULT_OCTAVE")));
@property (readonly) int32_t DEFAULT_TEMPO_BPM __attribute__((swift_name("DEFAULT_TEMPO_BPM")));
@property (readonly) int32_t DEFAULT_TICKS_PER_BEAT __attribute__((swift_name("DEFAULT_TICKS_PER_BEAT")));
@property (readonly) int32_t HEADER_CHUNK_LENGTH __attribute__((swift_name("HEADER_CHUNK_LENGTH")));
@property (readonly) int32_t MAX_PITCH __attribute__((swift_name("MAX_PITCH")));
@property (readonly) int32_t MAX_VELOCITY __attribute__((swift_name("MAX_VELOCITY")));
@property (readonly) int32_t MIDDLE_C_MIDI_NOTE __attribute__((swift_name("MIDDLE_C_MIDI_NOTE")));
@property (readonly) int32_t OCTAVE_DIVISOR __attribute__((swift_name("OCTAVE_DIVISOR")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.PerformanceThresholds")))
@interface SharedWWWGlobalsPerformanceThresholds : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)performanceThresholds __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsPerformanceThresholds *shared __attribute__((swift_name("shared")));
@property (readonly) double MEMORY_USAGE_LOW __attribute__((swift_name("MEMORY_USAGE_LOW")));
@property (readonly) double MEMORY_USAGE_MEDIUM __attribute__((swift_name("MEMORY_USAGE_MEDIUM")));
@property (readonly) double PARTICIPATION_RATE_HIGH __attribute__((swift_name("PARTICIPATION_RATE_HIGH")));
@property (readonly) double PARTICIPATION_RATE_MEDIUM __attribute__((swift_name("PARTICIPATION_RATE_MEDIUM")));
@property (readonly) double TIMING_ACCURACY_EXCELLENT __attribute__((swift_name("TIMING_ACCURACY_EXCELLENT")));
@property (readonly) double TIMING_ACCURACY_GOOD __attribute__((swift_name("TIMING_ACCURACY_GOOD")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.SpatialIndex")))
@interface SharedWWWGlobalsSpatialIndex : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)spatialIndex __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsSpatialIndex *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DEFAULT_GRID_SIZE __attribute__((swift_name("DEFAULT_GRID_SIZE")));
@property (readonly) int32_t MIN_ADAPTIVE_GRID_SIZE __attribute__((swift_name("MIN_ADAPTIVE_GRID_SIZE")));
@property (readonly) int32_t POLYGON_SIZE_DIVISOR __attribute__((swift_name("POLYGON_SIZE_DIVISOR")));
@property (readonly) int32_t SPATIAL_OPTIMIZATION_THRESHOLD __attribute__((swift_name("SPATIAL_OPTIMIZATION_THRESHOLD")));
@property (readonly) int32_t TRIG_CACHE_MAX_SIZE __attribute__((swift_name("TRIG_CACHE_MAX_SIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.TabBar")))
@interface SharedWWWGlobalsTabBar : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)tabBar __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsTabBar *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t EXT_HEIGHT __attribute__((swift_name("EXT_HEIGHT")));
@property (readonly) int32_t INT_HEIGHT __attribute__((swift_name("INT_HEIGHT")));
@property (readonly) int32_t INT_ITEM_FONTSIZE __attribute__((swift_name("INT_ITEM_FONTSIZE")));
@property (readonly) int32_t INT_ITEM_WIDTH __attribute__((swift_name("INT_ITEM_WIDTH")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Timing")))
@interface SharedWWWGlobalsTiming : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)timing __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsTiming *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t GPS_PERMISSION_REASK_DELAY __attribute__((swift_name("GPS_PERMISSION_REASK_DELAY")));
@property (readonly) int64_t GPS_UPDATE_INTERVAL __attribute__((swift_name("GPS_UPDATE_INTERVAL")));
@property (readonly) int32_t MAP_CAMERA_ANIMATION_DURATION_MS __attribute__((swift_name("MAP_CAMERA_ANIMATION_DURATION_MS")));
@property (readonly) int64_t SPLASH_MAX_DURATION __attribute__((swift_name("SPLASH_MAX_DURATION")));
@property (readonly) int64_t SPLASH_MIN_DURATION __attribute__((swift_name("SPLASH_MIN_DURATION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Urls")))
@interface SharedWWWGlobalsUrls : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)urls __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsUrls *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *INSTAGRAM_BASE __attribute__((swift_name("INSTAGRAM_BASE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.Wave")))
@interface SharedWWWGlobalsWave : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)wave __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsWave *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BACKGROUND_COLOR __attribute__((swift_name("BACKGROUND_COLOR")));
@property (readonly) float BACKGROUND_OPACITY __attribute__((swift_name("BACKGROUND_OPACITY")));
@property (readonly) int32_t DEFAULT_SPEED_SIMULATION __attribute__((swift_name("DEFAULT_SPEED_SIMULATION")));
@property (readonly) double LINEAR_METERS_REFRESH __attribute__((swift_name("LINEAR_METERS_REFRESH")));
@property (readonly) int32_t MAX_SIMULATION_SPEED __attribute__((swift_name("MAX_SIMULATION_SPEED")));
@property (readonly) int32_t MIN_SIMULATION_SPEED __attribute__((swift_name("MIN_SIMULATION_SPEED")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.WaveDisplay")))
@interface SharedWWWGlobalsWaveDisplay : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)waveDisplay __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsWaveDisplay *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t BEREADY_FONTSIZE __attribute__((swift_name("BEREADY_FONTSIZE")));
@property (readonly) int32_t BEREADY_PADDING __attribute__((swift_name("BEREADY_PADDING")));
@property (readonly) int32_t PROGRESSION_FONTSIZE __attribute__((swift_name("PROGRESSION_FONTSIZE")));
@property (readonly) int32_t PROGRESSION_HEIGHT __attribute__((swift_name("PROGRESSION_HEIGHT")));
@property (readonly) int32_t TIMEBEFOREHIT_FONTSIZE __attribute__((swift_name("TIMEBEFOREHIT_FONTSIZE")));
@property (readonly) int32_t TRIANGLE_SIZE __attribute__((swift_name("TRIANGLE_SIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobals.WaveTiming")))
@interface SharedWWWGlobalsWaveTiming : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)waveTiming __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWGlobalsWaveTiming *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t OBSERVE_DELAY __attribute__((swift_name("OBSERVE_DELAY")));
@property (readonly) int64_t SHOW_HIT_SEQUENCE_SECONDS __attribute__((swift_name("SHOW_HIT_SEQUENCE_SECONDS")));
@property (readonly) int64_t SOON_DELAY __attribute__((swift_name("SOON_DELAY")));
@property (readonly) int64_t WARMING_DURATION __attribute__((swift_name("WARMING_DURATION")));
@property (readonly) int64_t WARN_BEFORE_HIT __attribute__((swift_name("WARN_BEFORE_HIT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWPlatform")))
@interface SharedWWWPlatform : SharedBase
- (instancetype)initWithName:(NSString *)name positionManager:(SharedPositionManager * _Nullable)positionManager __attribute__((swift_name("init(name:positionManager:)"))) __attribute__((objc_designated_initializer));
- (void)disableSimulation __attribute__((swift_name("disableSimulation()")));
- (void)disableSimulationMode __attribute__((swift_name("disableSimulationMode()")));
- (void)enableSimulationMode __attribute__((swift_name("enableSimulationMode()")));
- (SharedWWWSimulation * _Nullable)getSimulation __attribute__((swift_name("getSimulation()")));
- (BOOL)isOnSimulation __attribute__((swift_name("isOnSimulation()")));
- (void)setSimulationSimulation:(SharedWWWSimulation *)simulation __attribute__((swift_name("setSimulation(simulation:)")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> simulationChanged __attribute__((swift_name("simulationChanged")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> simulationModeEnabled __attribute__((swift_name("simulationModeEnabled")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWShutdownHandler")))
@interface SharedWWWShutdownHandler : SharedBase
- (instancetype)initWithCoroutineScopeProvider:(id<SharedCoroutineScopeProvider>)coroutineScopeProvider __attribute__((swift_name("init(coroutineScopeProvider:)"))) __attribute__((objc_designated_initializer));
- (void)onAppShutdown __attribute__((swift_name("onAppShutdown()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWSimulation")))
@interface SharedWWWSimulation : SharedBase
- (instancetype)initWithStartDateTime:(SharedKotlinInstant *)startDateTime userPosition:(SharedPosition *)userPosition initialSpeed:(int32_t)initialSpeed __attribute__((swift_name("init(startDateTime:userPosition:initialSpeed:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWSimulationCompanion *companion __attribute__((swift_name("companion")));
- (SharedPosition *)getUserPosition __attribute__((swift_name("getUserPosition()")));
- (SharedKotlinInstant *)now __attribute__((swift_name("now()")));
- (void)pause __attribute__((swift_name("pause()")));
- (void)reset __attribute__((swift_name("reset()")));
- (void)resumeResumeSpeed:(int32_t)resumeSpeed __attribute__((swift_name("resume(resumeSpeed:)")));
- (int32_t)setSpeedNewSpeed:(int32_t)newSpeed __attribute__((swift_name("setSpeed(newSpeed:)")));
@property (readonly) int32_t speed __attribute__((swift_name("speed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWSimulation.Companion")))
@interface SharedWWWSimulationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWSimulationCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t MAX_SPEED __attribute__((swift_name("MAX_SPEED")));
@property (readonly) int32_t MIN_SPEED __attribute__((swift_name("MIN_SPEED")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyDefinition")))
@interface SharedChoreographyDefinition : SharedBase
- (instancetype)initWithWarmingSequences:(NSArray<SharedChoreographySequence *> *)warmingSequences waitingSequence:(SharedChoreographySequence * _Nullable)waitingSequence hitSequence:(SharedChoreographySequence * _Nullable)hitSequence __attribute__((swift_name("init(warmingSequences:waitingSequence:hitSequence:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedChoreographyDefinitionCompanion *companion __attribute__((swift_name("companion")));
- (SharedChoreographyDefinition *)doCopyWarmingSequences:(NSArray<SharedChoreographySequence *> *)warmingSequences waitingSequence:(SharedChoreographySequence * _Nullable)waitingSequence hitSequence:(SharedChoreographySequence * _Nullable)hitSequence __attribute__((swift_name("doCopy(warmingSequences:waitingSequence:hitSequence:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="hit_sequence")
*/
@property (readonly) SharedChoreographySequence * _Nullable hitSequence __attribute__((swift_name("hitSequence")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="waiting_sequence")
*/
@property (readonly) SharedChoreographySequence * _Nullable waitingSequence __attribute__((swift_name("waitingSequence")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="warming_sequences")
*/
@property (readonly) NSArray<SharedChoreographySequence *> *warmingSequences __attribute__((swift_name("warmingSequences")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyDefinition.Companion")))
@interface SharedChoreographyDefinitionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChoreographyDefinitionCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("Koin_coreKoinComponent")))
@protocol SharedKoin_coreKoinComponent
@required
- (SharedKoin_coreKoin *)getKoin __attribute__((swift_name("getKoin()")));
@end

__attribute__((swift_name("ChoreographyManager")))
@interface SharedChoreographyManager<T> : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)clearImageCache __attribute__((swift_name("clearImageCache()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCurrentWarmingSequenceStartTime:(SharedKotlinInstant *)startTime completionHandler:(void (^)(SharedChoreographyManagerDisplayableSequence<T> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getCurrentWarmingSequence(startTime:completionHandler:)")));
- (SharedChoreographyManagerDisplayableSequence<T> * _Nullable)getCurrentWarmingSequenceImmediateStartTime:(SharedKotlinInstant *)startTime __attribute__((swift_name("getCurrentWarmingSequenceImmediate(startTime:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getHitSequenceWithCompletionHandler:(void (^)(SharedChoreographyManagerDisplayableSequence<T> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getHitSequence(completionHandler:)")));
- (SharedChoreographyManagerDisplayableSequence<T> * _Nullable)getHitSequenceImmediate __attribute__((swift_name("getHitSequenceImmediate()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWaitingSequenceWithCompletionHandler:(void (^)(SharedChoreographyManagerDisplayableSequence<T> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getWaitingSequence(completionHandler:)")));
- (SharedChoreographyManagerDisplayableSequence<T> * _Nullable)getWaitingSequenceImmediate __attribute__((swift_name("getWaitingSequenceImmediate()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)preloadForWaveSyncWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("preloadForWaveSync(completionHandler:)")));
@property (readonly) id<SharedIClock> clock __attribute__((swift_name("clock")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyManagerDisplayableSequence")))
@interface SharedChoreographyManagerDisplayableSequence<T> : SharedBase
- (instancetype)initWithImage:(T _Nullable)image frameWidth:(int32_t)frameWidth frameHeight:(int32_t)frameHeight frameCount:(int32_t)frameCount timing:(int64_t)timing duration:(int64_t)duration text:(SharedResourcesStringResource *)text loop:(BOOL)loop remainingDuration:(id _Nullable)remainingDuration __attribute__((swift_name("init(image:frameWidth:frameHeight:frameCount:timing:duration:text:loop:remainingDuration:)"))) __attribute__((objc_designated_initializer));
- (SharedChoreographyManagerDisplayableSequence<T> *)doCopyImage:(T _Nullable)image frameWidth:(int32_t)frameWidth frameHeight:(int32_t)frameHeight frameCount:(int32_t)frameCount timing:(int64_t)timing duration:(int64_t)duration text:(SharedResourcesStringResource *)text loop:(BOOL)loop remainingDuration:(id _Nullable)remainingDuration __attribute__((swift_name("doCopy(image:frameWidth:frameHeight:frameCount:timing:duration:text:loop:remainingDuration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t duration __attribute__((swift_name("duration")));
@property (readonly) int32_t frameCount __attribute__((swift_name("frameCount")));
@property (readonly) int32_t frameHeight __attribute__((swift_name("frameHeight")));
@property (readonly) int32_t frameWidth __attribute__((swift_name("frameWidth")));
@property (readonly) T _Nullable image __attribute__((swift_name("image")));
@property (readonly) BOOL loop __attribute__((swift_name("loop")));
@property (readonly) id _Nullable remainingDuration __attribute__((swift_name("remainingDuration")));
@property (readonly) SharedResourcesStringResource *text __attribute__((swift_name("text")));
@property (readonly) int64_t timing __attribute__((swift_name("timing")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyManagerResolvedChoreography")))
@interface SharedChoreographyManagerResolvedChoreography<T> : SharedBase
- (instancetype)initWithWarmingSequences:(NSArray<SharedChoreographyManagerResolvedSequence<T> *> *)warmingSequences waitingSequence:(SharedChoreographyManagerResolvedSequence<T> * _Nullable)waitingSequence hitSequence:(SharedChoreographyManagerResolvedSequence<T> * _Nullable)hitSequence __attribute__((swift_name("init(warmingSequences:waitingSequence:hitSequence:)"))) __attribute__((objc_designated_initializer));
- (SharedChoreographyManagerResolvedChoreography<T> *)doCopyWarmingSequences:(NSArray<SharedChoreographyManagerResolvedSequence<T> *> *)warmingSequences waitingSequence:(SharedChoreographyManagerResolvedSequence<T> * _Nullable)waitingSequence hitSequence:(SharedChoreographyManagerResolvedSequence<T> * _Nullable)hitSequence __attribute__((swift_name("doCopy(warmingSequences:waitingSequence:hitSequence:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedChoreographyManagerResolvedSequence<T> * _Nullable hitSequence __attribute__((swift_name("hitSequence")));
@property (readonly) SharedChoreographyManagerResolvedSequence<T> * _Nullable waitingSequence __attribute__((swift_name("waitingSequence")));
@property (readonly) NSArray<SharedChoreographyManagerResolvedSequence<T> *> *warmingSequences __attribute__((swift_name("warmingSequences")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyManagerResolvedSequence")))
@interface SharedChoreographyManagerResolvedSequence<T> : SharedBase
- (instancetype)initWithSequence:(SharedChoreographySequence *)sequence text:(SharedResourcesStringResource *)text resolvedImage:(T _Nullable)resolvedImage startTime:(int64_t)startTime endTime:(int64_t)endTime __attribute__((swift_name("init(sequence:text:resolvedImage:startTime:endTime:)"))) __attribute__((objc_designated_initializer));
- (SharedChoreographyManagerResolvedSequence<T> *)doCopySequence:(SharedChoreographySequence *)sequence text:(SharedResourcesStringResource *)text resolvedImage:(T _Nullable)resolvedImage startTime:(int64_t)startTime endTime:(int64_t)endTime __attribute__((swift_name("doCopy(sequence:text:resolvedImage:startTime:endTime:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t endTime __attribute__((swift_name("endTime")));
@property (readonly) T _Nullable resolvedImage __attribute__((swift_name("resolvedImage")));
@property (readonly) SharedChoreographySequence *sequence __attribute__((swift_name("sequence")));
@property (readonly) int64_t startTime __attribute__((swift_name("startTime")));
@property (readonly) SharedResourcesStringResource *text __attribute__((swift_name("text")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographySequence")))
@interface SharedChoreographySequence : SharedBase
- (instancetype)initWithFrames:(NSString *)frames frameWidth:(int32_t)frameWidth frameHeight:(int32_t)frameHeight frameCount:(int32_t)frameCount timing:(int64_t)timing loop:(BOOL)loop duration:(id _Nullable)duration __attribute__((swift_name("init(frames:frameWidth:frameHeight:frameCount:timing:loop:duration:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedChoreographySequenceCompanion *companion __attribute__((swift_name("companion")));
- (SharedChoreographySequence *)doCopyFrames:(NSString *)frames frameWidth:(int32_t)frameWidth frameHeight:(int32_t)frameHeight frameCount:(int32_t)frameCount timing:(int64_t)timing loop:(BOOL)loop duration:(id _Nullable)duration __attribute__((swift_name("doCopy(frames:frameWidth:frameHeight:frameCount:timing:loop:duration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSArray<id> *)resolveImageResourcesResolver:(id<SharedImageResolver>)resolver __attribute__((swift_name("resolveImageResources(resolver:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id _Nullable duration __attribute__((swift_name("duration")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="frame_count")
*/
@property (readonly) int32_t frameCount __attribute__((swift_name("frameCount")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="frame_height")
*/
@property (readonly) int32_t frameHeight __attribute__((swift_name("frameHeight")));

/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="frame_width")
*/
@property (readonly) int32_t frameWidth __attribute__((swift_name("frameWidth")));
@property (readonly) NSString *frames __attribute__((swift_name("frames")));
@property (readonly) BOOL loop __attribute__((swift_name("loop")));
@property (readonly) int64_t timing __attribute__((swift_name("timing")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographySequence.Companion")))
@interface SharedChoreographySequenceCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChoreographySequenceCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SoundChoreographyManager")))
@interface SharedSoundChoreographyManager : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)initWithCoroutineScopeProvider:(id<SharedCoroutineScopeProvider>)coroutineScopeProvider __attribute__((swift_name("init(coroutineScopeProvider:)"))) __attribute__((objc_designated_initializer));
- (int64_t)getTotalDuration __attribute__((swift_name("getTotalDuration()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)playCurrentSoundToneWaveStartTime:(SharedKotlinInstant *)waveStartTime completionHandler:(void (^)(SharedInt * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("playCurrentSoundTone(waveStartTime:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)preloadMidiFileMidiResourcePath:(NSString *)midiResourcePath completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("preloadMidiFile(midiResourcePath:completionHandler:)")));
- (void)release_ __attribute__((swift_name("release()")));
- (void)setCurrentTrackTrack:(SharedMidiTrack *)track __attribute__((swift_name("setCurrentTrack(track:)")));
- (void)setLoopingLoop:(BOOL)loop __attribute__((swift_name("setLooping(loop:)")));
- (void)setWaveformWaveform:(SharedSoundPlayerWaveform *)waveform __attribute__((swift_name("setWaveform(waveform:)")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface SharedKotlinThrowable : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (SharedKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface SharedKotlinException : SharedKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataStoreException")))
@interface SharedDataStoreException : SharedKotlinException
- (instancetype)initWithMessage:(NSString *)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end

__attribute__((swift_name("DataStoreFactory")))
@protocol SharedDataStoreFactory
@required
- (id<SharedDatastore_coreDataStore>)createProducePath:(NSString *(^)(void))producePath __attribute__((swift_name("create(producePath:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultDataStoreFactory")))
@interface SharedDefaultDataStoreFactory : SharedBase <SharedDataStoreFactory>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<SharedDatastore_coreDataStore>)createProducePath:(NSString *(^)(void))producePath __attribute__((swift_name("create(producePath:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FavoriteEventsStore")))
@interface SharedFavoriteEventsStore : SharedBase
- (instancetype)initWithDataStore:(id<SharedDatastore_coreDataStore>)dataStore dispatcher:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)dispatcher __attribute__((swift_name("init(dataStore:dispatcher:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isFavoriteEventId:(NSString *)eventId completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isFavorite(eventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)setFavoriteStatusEventId:(NSString *)eventId isFavorite:(BOOL)isFavorite completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("setFavoriteStatus(eventId:isFavorite:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HiddenMapsStore")))
@interface SharedHiddenMapsStore : SharedBase
- (instancetype)initWithDataStore:(id<SharedDatastore_coreDataStore>)dataStore dispatcher:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)dispatcher __attribute__((swift_name("init(dataStore:dispatcher:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedHiddenMapsStoreCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addMapId:(NSString *)mapId completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("add(mapId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getAllWithCompletionHandler:(void (^)(NSSet<NSString *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getAll(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isHiddenMapId:(NSString *)mapId completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isHidden(mapId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)removeMapId:(NSString *)mapId completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("remove(mapId:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HiddenMapsStore.Companion")))
@interface SharedHiddenMapsStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedHiddenMapsStoreCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InitFavoriteEvent")))
@interface SharedInitFavoriteEvent : SharedBase
- (instancetype)initWithFavoriteEventsStore:(SharedFavoriteEventsStore *)favoriteEventsStore __attribute__((swift_name("init(favoriteEventsStore:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)callEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("call(event:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SetEventFavorite")))
@interface SharedSetEventFavorite : SharedBase
- (instancetype)initWithFavoriteEventsStore:(SharedFavoriteEventsStore *)favoriteEventsStore __attribute__((swift_name("init(favoriteEventsStore:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)callEvent:(id<SharedIWWWEvent>)event isFavorite:(BOOL)isFavorite completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("call(event:isFavorite:completionHandler:)")));
@end


/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TestDataStoreFactory")))
@interface SharedTestDataStoreFactory : SharedBase <SharedDataStoreFactory>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<SharedDatastore_coreDataStore>)createProducePath:(NSString *(^)(void))producePath __attribute__((swift_name("create(producePath:)")));
@end

__attribute__((swift_name("PositionObserver")))
@protocol SharedPositionObserver
@required
- (double)calculateDistanceFrom:(SharedPosition *)from to:(SharedPosition *)to __attribute__((swift_name("calculateDistance(from:to:)")));
- (SharedPosition * _Nullable)getCurrentPosition __attribute__((swift_name("getCurrentPosition()")));
- (BOOL)isObserving __attribute__((swift_name("isObserving()")));
- (BOOL)isValidPositionPosition:(SharedPosition *)position __attribute__((swift_name("isValidPosition(position:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)observePositionForEventEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("observePositionForEvent(event:)")));
- (void)stopObservation __attribute__((swift_name("stopObservation()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultPositionObserver")))
@interface SharedDefaultPositionObserver : SharedBase <SharedPositionObserver>
- (instancetype)initWithPositionManager:(SharedPositionManager *)positionManager waveProgressionTracker:(id<SharedWaveProgressionTracker>)waveProgressionTracker clock:(id<SharedIClock>)clock __attribute__((swift_name("init(positionManager:waveProgressionTracker:clock:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDefaultPositionObserverCompanion *companion __attribute__((swift_name("companion")));
- (double)calculateDistanceFrom:(SharedPosition *)from to:(SharedPosition *)to __attribute__((swift_name("calculateDistance(from:to:)")));
- (SharedPosition * _Nullable)getCurrentPosition __attribute__((swift_name("getCurrentPosition()")));
- (BOOL)isObserving __attribute__((swift_name("isObserving()")));
- (BOOL)isValidPositionPosition:(SharedPosition *)position __attribute__((swift_name("isValidPosition(position:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)observePositionForEventEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("observePositionForEvent(event:)")));
- (void)stopObservation __attribute__((swift_name("stopObservation()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultPositionObserver.Companion")))
@interface SharedDefaultPositionObserverCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDefaultPositionObserverCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PositionObservation")))
@interface SharedPositionObservation : SharedBase
- (instancetype)initWithPosition:(SharedPosition * _Nullable)position isInArea:(BOOL)isInArea timestamp:(SharedKotlinInstant *)timestamp __attribute__((swift_name("init(position:isInArea:timestamp:)"))) __attribute__((objc_designated_initializer));
- (SharedPositionObservation *)doCopyPosition:(SharedPosition * _Nullable)position isInArea:(BOOL)isInArea timestamp:(SharedKotlinInstant *)timestamp __attribute__((swift_name("doCopy(position:isInArea:timestamp:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isInArea __attribute__((swift_name("isInArea")));
@property (readonly) SharedPosition * _Nullable position __attribute__((swift_name("position")));
@property (readonly) SharedKotlinInstant *timestamp __attribute__((swift_name("timestamp")));
@end

__attribute__((swift_name("WaveProgressionTracker")))
@protocol SharedWaveProgressionTracker
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateProgressionEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateProgression(event:completionHandler:)")));
- (void)clearProgressionHistory __attribute__((swift_name("clearProgressionHistory()")));
- (NSArray<SharedProgressionSnapshot *> *)getProgressionHistory __attribute__((swift_name("getProgressionHistory()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isUserInWaveAreaUserPosition:(SharedPosition *)userPosition waveArea:(SharedWWWEventArea *)waveArea completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isUserInWaveArea(userPosition:waveArea:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)recordProgressionSnapshotEvent:(id<SharedIWWWEvent>)event userPosition:(SharedPosition * _Nullable)userPosition completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("recordProgressionSnapshot(event:userPosition:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultWaveProgressionTracker")))
@interface SharedDefaultWaveProgressionTracker : SharedBase <SharedWaveProgressionTracker>
- (instancetype)initWithClock:(id<SharedIClock>)clock __attribute__((swift_name("init(clock:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateProgressionEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateProgression(event:completionHandler:)")));
- (void)clearProgressionHistory __attribute__((swift_name("clearProgressionHistory()")));
- (NSArray<SharedProgressionSnapshot *> *)getProgressionHistory __attribute__((swift_name("getProgressionHistory()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isUserInWaveAreaUserPosition:(SharedPosition *)userPosition waveArea:(SharedWWWEventArea *)waveArea completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isUserInWaveArea(userPosition:waveArea:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)recordProgressionSnapshotEvent:(id<SharedIWWWEvent>)event userPosition:(SharedPosition * _Nullable)userPosition completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("recordProgressionSnapshot(event:userPosition:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProgressionSnapshot")))
@interface SharedProgressionSnapshot : SharedBase
- (instancetype)initWithTimestamp:(SharedKotlinInstant *)timestamp progression:(double)progression userPosition:(SharedPosition * _Nullable)userPosition isInWaveArea:(BOOL)isInWaveArea __attribute__((swift_name("init(timestamp:progression:userPosition:isInWaveArea:)"))) __attribute__((objc_designated_initializer));
- (SharedProgressionSnapshot *)doCopyTimestamp:(SharedKotlinInstant *)timestamp progression:(double)progression userPosition:(SharedPosition * _Nullable)userPosition isInWaveArea:(BOOL)isInWaveArea __attribute__((swift_name("doCopy(timestamp:progression:userPosition:isInWaveArea:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isInWaveArea __attribute__((swift_name("isInWaveArea")));
@property (readonly) double progression __attribute__((swift_name("progression")));
@property (readonly) SharedKotlinInstant *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) SharedPosition * _Nullable userPosition __attribute__((swift_name("userPosition")));
@end

__attribute__((swift_name("EventsRepository")))
@protocol SharedEventsRepository
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)clearCacheWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("clearCache(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCachedEventsCountWithCompletionHandler:(void (^)(SharedInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getCachedEventsCount(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEventEventId:(NSString *)eventId completionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEvent(eventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEventsWithCompletionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEvents(completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)getLastError __attribute__((swift_name("getLastError()")));
- (id<SharedKotlinx_coroutines_coreFlow>)isLoading __attribute__((swift_name("isLoading()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loadEventsOnLoadingError:(void (^)(SharedKotlinException *))onLoadingError completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("loadEvents(onLoadingError:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshEventsWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("refreshEvents(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsRepositoryImpl")))
@interface SharedEventsRepositoryImpl : SharedBase <SharedEventsRepository>
- (instancetype)initWithWwwEvents:(SharedWWWEvents *)wwwEvents __attribute__((swift_name("init(wwwEvents:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)clearCacheWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("clearCache(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCachedEventsCountWithCompletionHandler:(void (^)(SharedInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getCachedEventsCount(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEventEventId:(NSString *)eventId completionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEvent(eventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEventsWithCompletionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEvents(completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)getLastError __attribute__((swift_name("getLastError()")));
- (id<SharedKotlinx_coroutines_coreFlow>)isLoading __attribute__((swift_name("isLoading()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loadEventsOnLoadingError:(void (^)(SharedKotlinException *))onLoadingError completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("loadEvents(onLoadingError:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshEventsWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("refreshEvents(completionHandler:)")));
@end

__attribute__((swift_name("ObservationScheduler")))
@protocol SharedObservationScheduler
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateObservationIntervalEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateObservationInterval(event:completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)createObservationFlowEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("createObservationFlow(event:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getObservationScheduleEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedObservationSchedule * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getObservationSchedule(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)shouldObserveContinuouslyEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("shouldObserveContinuously(event:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultObservationScheduler")))
@interface SharedDefaultObservationScheduler : SharedBase <SharedObservationScheduler>
- (instancetype)initWithClock:(id<SharedIClock>)clock __attribute__((swift_name("init(clock:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateObservationIntervalEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateObservationInterval(event:completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)createObservationFlowEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("createObservationFlow(event:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getObservationScheduleEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedObservationSchedule * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getObservationSchedule(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)shouldObserveContinuouslyEvent:(id<SharedIWWWEvent>)event completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("shouldObserveContinuously(event:completionHandler:)")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedKotlinEnum<E> : SharedBase <SharedKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObservationPhase")))
@interface SharedObservationPhase : SharedKotlinEnum<SharedObservationPhase *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedObservationPhase *distant __attribute__((swift_name("distant")));
@property (class, readonly) SharedObservationPhase *approaching __attribute__((swift_name("approaching")));
@property (class, readonly) SharedObservationPhase *near __attribute__((swift_name("near")));
@property (class, readonly) SharedObservationPhase *active __attribute__((swift_name("active")));
@property (class, readonly) SharedObservationPhase *critical __attribute__((swift_name("critical")));
@property (class, readonly) SharedObservationPhase *inactive __attribute__((swift_name("inactive")));
+ (SharedKotlinArray<SharedObservationPhase *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedObservationPhase *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ObservationSchedule")))
@interface SharedObservationSchedule : SharedBase
- (instancetype)initWithShouldObserve:(BOOL)shouldObserve interval:(int64_t)interval phase:(SharedObservationPhase *)phase nextObservationTime:(SharedKotlinInstant * _Nullable)nextObservationTime reason:(NSString *)reason __attribute__((swift_name("init(shouldObserve:interval:phase:nextObservationTime:reason:)"))) __attribute__((objc_designated_initializer));
- (SharedObservationSchedule *)doCopyShouldObserve:(BOOL)shouldObserve interval:(int64_t)interval phase:(SharedObservationPhase *)phase nextObservationTime:(SharedKotlinInstant * _Nullable)nextObservationTime reason:(NSString *)reason __attribute__((swift_name("doCopy(shouldObserve:interval:phase:nextObservationTime:reason:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t interval __attribute__((swift_name("interval")));
@property (readonly) SharedKotlinInstant * _Nullable nextObservationTime __attribute__((swift_name("nextObservationTime")));
@property (readonly) SharedObservationPhase *phase __attribute__((swift_name("phase")));
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
@property (readonly) BOOL shouldObserve __attribute__((swift_name("shouldObserve")));
@end

__attribute__((swift_name("EventStateManager")))
@protocol SharedEventStateManager
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateEventStateEvent:(id<SharedIWWWEvent>)event input:(SharedEventStateInput *)input userIsInArea:(BOOL)userIsInArea completionHandler:(void (^)(SharedEventState * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateEventState(event:input:userIsInArea:completionHandler:)")));
- (NSArray<SharedStateValidationIssue *> *)validateStateInput:(SharedEventStateInput *)input calculatedState:(SharedEventState *)calculatedState __attribute__((swift_name("validateState(input:calculatedState:)")));
- (NSArray<SharedStateValidationIssue *> *)validateStateTransitionPreviousState:(SharedEventState * _Nullable)previousState newState:(SharedEventState *)newState __attribute__((swift_name("validateStateTransition(previousState:newState:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultEventStateManager")))
@interface SharedDefaultEventStateManager : SharedBase <SharedEventStateManager>
- (instancetype)initWithWaveProgressionTracker:(id<SharedWaveProgressionTracker>)waveProgressionTracker clock:(id<SharedIClock>)clock __attribute__((swift_name("init(waveProgressionTracker:clock:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)calculateEventStateEvent:(id<SharedIWWWEvent>)event input:(SharedEventStateInput *)input userIsInArea:(BOOL)userIsInArea completionHandler:(void (^)(SharedEventState * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("calculateEventState(event:input:userIsInArea:completionHandler:)")));
- (NSArray<SharedStateValidationIssue *> *)validateStateInput:(SharedEventStateInput *)input calculatedState:(SharedEventState *)calculatedState __attribute__((swift_name("validateState(input:calculatedState:)")));
- (NSArray<SharedStateValidationIssue *> *)validateStateTransitionPreviousState:(SharedEventState * _Nullable)previousState newState:(SharedEventState *)newState __attribute__((swift_name("validateStateTransition(previousState:newState:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventState")))
@interface SharedEventState : SharedBase
- (instancetype)initWithProgression:(double)progression status:(SharedIWWWEventStatus *)status isUserWarmingInProgress:(BOOL)isUserWarmingInProgress isStartWarmingInProgress:(BOOL)isStartWarmingInProgress userIsGoingToBeHit:(BOOL)userIsGoingToBeHit userHasBeenHit:(BOOL)userHasBeenHit userPositionRatio:(double)userPositionRatio timeBeforeHit:(int64_t)timeBeforeHit hitDateTime:(SharedKotlinInstant *)hitDateTime userIsInArea:(BOOL)userIsInArea timestamp:(SharedKotlinInstant *)timestamp __attribute__((swift_name("init(progression:status:isUserWarmingInProgress:isStartWarmingInProgress:userIsGoingToBeHit:userHasBeenHit:userPositionRatio:timeBeforeHit:hitDateTime:userIsInArea:timestamp:)"))) __attribute__((objc_designated_initializer));
- (SharedEventState *)doCopyProgression:(double)progression status:(SharedIWWWEventStatus *)status isUserWarmingInProgress:(BOOL)isUserWarmingInProgress isStartWarmingInProgress:(BOOL)isStartWarmingInProgress userIsGoingToBeHit:(BOOL)userIsGoingToBeHit userHasBeenHit:(BOOL)userHasBeenHit userPositionRatio:(double)userPositionRatio timeBeforeHit:(int64_t)timeBeforeHit hitDateTime:(SharedKotlinInstant *)hitDateTime userIsInArea:(BOOL)userIsInArea timestamp:(SharedKotlinInstant *)timestamp __attribute__((swift_name("doCopy(progression:status:isUserWarmingInProgress:isStartWarmingInProgress:userIsGoingToBeHit:userHasBeenHit:userPositionRatio:timeBeforeHit:hitDateTime:userIsInArea:timestamp:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinInstant *hitDateTime __attribute__((swift_name("hitDateTime")));
@property (readonly) BOOL isStartWarmingInProgress __attribute__((swift_name("isStartWarmingInProgress")));
@property (readonly) BOOL isUserWarmingInProgress __attribute__((swift_name("isUserWarmingInProgress")));
@property (readonly) double progression __attribute__((swift_name("progression")));
@property (readonly) SharedIWWWEventStatus *status __attribute__((swift_name("status")));
@property (readonly) int64_t timeBeforeHit __attribute__((swift_name("timeBeforeHit")));
@property (readonly) SharedKotlinInstant *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) BOOL userHasBeenHit __attribute__((swift_name("userHasBeenHit")));
@property (readonly) BOOL userIsGoingToBeHit __attribute__((swift_name("userIsGoingToBeHit")));
@property (readonly) BOOL userIsInArea __attribute__((swift_name("userIsInArea")));
@property (readonly) double userPositionRatio __attribute__((swift_name("userPositionRatio")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventStateInput")))
@interface SharedEventStateInput : SharedBase
- (instancetype)initWithProgression:(double)progression status:(SharedIWWWEventStatus *)status userPosition:(SharedPosition * _Nullable)userPosition currentTime:(SharedKotlinInstant *)currentTime __attribute__((swift_name("init(progression:status:userPosition:currentTime:)"))) __attribute__((objc_designated_initializer));
- (SharedEventStateInput *)doCopyProgression:(double)progression status:(SharedIWWWEventStatus *)status userPosition:(SharedPosition * _Nullable)userPosition currentTime:(SharedKotlinInstant *)currentTime __attribute__((swift_name("doCopy(progression:status:userPosition:currentTime:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinInstant *currentTime __attribute__((swift_name("currentTime")));
@property (readonly) double progression __attribute__((swift_name("progression")));
@property (readonly) SharedIWWWEventStatus *status __attribute__((swift_name("status")));
@property (readonly) SharedPosition * _Nullable userPosition __attribute__((swift_name("userPosition")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StateValidationIssue")))
@interface SharedStateValidationIssue : SharedBase
- (instancetype)initWithField:(NSString *)field issue:(NSString *)issue severity:(SharedStateValidationIssueSeverity *)severity __attribute__((swift_name("init(field:issue:severity:)"))) __attribute__((objc_designated_initializer));
- (SharedStateValidationIssue *)doCopyField:(NSString *)field issue:(NSString *)issue severity:(SharedStateValidationIssueSeverity *)severity __attribute__((swift_name("doCopy(field:issue:severity:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *field __attribute__((swift_name("field")));
@property (readonly) NSString *issue __attribute__((swift_name("issue")));
@property (readonly) SharedStateValidationIssueSeverity *severity __attribute__((swift_name("severity")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StateValidationIssue.Severity")))
@interface SharedStateValidationIssueSeverity : SharedKotlinEnum<SharedStateValidationIssueSeverity *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedStateValidationIssueSeverity *warning __attribute__((swift_name("warning")));
@property (class, readonly) SharedStateValidationIssueSeverity *error __attribute__((swift_name("error")));
+ (SharedKotlinArray<SharedStateValidationIssueSeverity *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedStateValidationIssueSeverity *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CheckEventFavoritesUseCase")))
@interface SharedCheckEventFavoritesUseCase : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getFavoriteEventsEvents:(NSArray<id<SharedIWWWEvent>> *)events completionHandler:(void (^)(NSArray<id<SharedIWWWEvent>> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getFavoriteEvents(events:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getFavoriteEventsCountEvents:(NSArray<id<SharedIWWWEvent>> *)events completionHandler:(void (^)(SharedInt * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getFavoriteEventsCount(events:completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)getFavoriteEventsCountFlowEventsFlow:(id<SharedKotlinx_coroutines_coreFlow>)eventsFlow __attribute__((swift_name("getFavoriteEventsCountFlow(eventsFlow:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasFavoriteEventsEvents:(NSArray<id<SharedIWWWEvent>> *)events completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasFavoriteEvents(events:completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreFlow>)hasFavoriteEventsFlowEventsFlow:(id<SharedKotlinx_coroutines_coreFlow>)eventsFlow __attribute__((swift_name("hasFavoriteEventsFlow(eventsFlow:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventFilterCriteria")))
@interface SharedEventFilterCriteria : SharedBase
- (instancetype)initWithOnlyFavorites:(BOOL)onlyFavorites onlyDownloaded:(BOOL)onlyDownloaded onlyRunning:(BOOL)onlyRunning onlyUpcoming:(BOOL)onlyUpcoming onlyCompleted:(BOOL)onlyCompleted eventIds:(NSArray<NSString *> * _Nullable)eventIds __attribute__((swift_name("init(onlyFavorites:onlyDownloaded:onlyRunning:onlyUpcoming:onlyCompleted:eventIds:)"))) __attribute__((objc_designated_initializer));
- (SharedEventFilterCriteria *)doCopyOnlyFavorites:(BOOL)onlyFavorites onlyDownloaded:(BOOL)onlyDownloaded onlyRunning:(BOOL)onlyRunning onlyUpcoming:(BOOL)onlyUpcoming onlyCompleted:(BOOL)onlyCompleted eventIds:(NSArray<NSString *> * _Nullable)eventIds __attribute__((swift_name("doCopy(onlyFavorites:onlyDownloaded:onlyRunning:onlyUpcoming:onlyCompleted:eventIds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> * _Nullable eventIds __attribute__((swift_name("eventIds")));
@property (readonly) BOOL onlyCompleted __attribute__((swift_name("onlyCompleted")));
@property (readonly) BOOL onlyDownloaded __attribute__((swift_name("onlyDownloaded")));
@property (readonly) BOOL onlyFavorites __attribute__((swift_name("onlyFavorites")));
@property (readonly) BOOL onlyRunning __attribute__((swift_name("onlyRunning")));
@property (readonly) BOOL onlyUpcoming __attribute__((swift_name("onlyUpcoming")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FilterEventsUseCase")))
@interface SharedFilterEventsUseCase : SharedBase
- (instancetype)initWithMapAvailabilityChecker:(id<SharedMapAvailabilityChecker>)mapAvailabilityChecker __attribute__((swift_name("init(mapAvailabilityChecker:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)filterEvents:(NSArray<id<SharedIWWWEvent>> *)events onlyFavorites:(BOOL)onlyFavorites onlyDownloaded:(BOOL)onlyDownloaded completionHandler:(void (^)(NSArray<id<SharedIWWWEvent>> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("filter(events:onlyFavorites:onlyDownloaded:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeEvents:(NSArray<id<SharedIWWWEvent>> *)events criteria:(SharedEventFilterCriteria *)criteria completionHandler:(void (^)(NSArray<id<SharedIWWWEvent>> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(events:criteria:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetSortedEventsUseCase")))
@interface SharedGetSortedEventsUseCase : SharedBase
- (instancetype)initWithEventsRepository:(id<SharedEventsRepository>)eventsRepository __attribute__((swift_name("init(eventsRepository:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeLimit:(SharedInt * _Nullable)limit completionHandler:(void (^)(id<SharedKotlinx_coroutines_coreFlow> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(limit:completionHandler:)")));
@end

__attribute__((swift_name("MapAvailabilityChecker")))
@protocol SharedMapAvailabilityChecker
@required
- (NSArray<NSString *> *)getDownloadedMaps __attribute__((swift_name("getDownloadedMaps()")));
- (BOOL)isMapDownloadedEventId:(NSString *)eventId __attribute__((swift_name("isMapDownloaded(eventId:)")));
- (void)refreshAvailability __attribute__((swift_name("refreshAvailability()")));
@end

__attribute__((swift_name("DataValidator")))
@protocol SharedDataValidator
@required
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@end

__attribute__((swift_name("IWWWEvent")))
@protocol SharedIWWWEvent <SharedDataValidator>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getAllNumbersWithCompletionHandler:(void (^)(SharedIWWWEventWaveNumbersLiterals * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getAllNumbers(completionHandler:)")));
- (id _Nullable)getCommunityImage __attribute__((swift_name("getCommunityImage()")));
- (id _Nullable)getCountryImage __attribute__((swift_name("getCountryImage()")));
- (SharedResourcesStringResource *)getDescription __attribute__((swift_name("getDescription()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEndDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEndDateTime(completionHandler:)")));
- (SharedWWWEventObserver *)getEventObserver __attribute__((swift_name("getEventObserver()")));
- (SharedResourcesStringResource *)getLiteralCommunity __attribute__((swift_name("getLiteralCommunity()")));
- (SharedResourcesStringResource *)getLiteralCountry __attribute__((swift_name("getLiteralCountry()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getLiteralEndTimeWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getLiteralEndTime(completionHandler:)")));
- (NSString *)getLiteralStartDateSimple __attribute__((swift_name("getLiteralStartDateSimple()")));
- (NSString *)getLiteralStartTime __attribute__((swift_name("getLiteralStartTime()")));
- (NSString *)getLiteralTimezone __attribute__((swift_name("getLiteralTimezone()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getLiteralTotalTimeWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getLiteralTotalTime(completionHandler:)")));
- (SharedResourcesStringResource *)getLocation __attribute__((swift_name("getLocation()")));
- (id _Nullable)getLocationImage __attribute__((swift_name("getLocationImage()")));
- (id _Nullable)getMapImage __attribute__((swift_name("getMapImage()")));
- (SharedKotlinInstant *)getStartDateTime __attribute__((swift_name("getStartDateTime()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getStatusWithCompletionHandler:(void (^)(SharedIWWWEventStatus * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getStatus(completionHandler:)")));
- (SharedKotlinx_datetimeTimeZone *)getTZ __attribute__((swift_name("getTZ()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTotalTimeWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getTotalTime(completionHandler:)")));
- (int64_t)getWarmingDuration __attribute__((swift_name("getWarmingDuration()")));
- (SharedKotlinInstant *)getWaveStartDateTime __attribute__((swift_name("getWaveStartDateTime()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isDoneWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isDone(completionHandler:)")));
- (BOOL)isNearTime __attribute__((swift_name("isNearTime()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isRunningWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isRunning(completionHandler:)")));
- (BOOL)isSoon __attribute__((swift_name("isSoon()")));
@property (readonly) SharedWWWEventArea *area __attribute__((swift_name("area")));
@property (readonly) NSString * _Nullable community __attribute__((swift_name("community")));
@property (readonly) NSString * _Nullable country __attribute__((swift_name("country")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property BOOL favorite __attribute__((swift_name("favorite")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *instagramAccount __attribute__((swift_name("instagramAccount")));
@property (readonly) NSString *instagramHashtag __attribute__((swift_name("instagramHashtag")));
@property (readonly) SharedWWWEventMap *map __attribute__((swift_name("map")));

/**
 * @note annotations
 *   kotlinx.serialization.Transient
*/
@property (readonly) SharedWWWEventObserver *observer __attribute__((swift_name("observer")));
@property (readonly) NSString *startHour __attribute__((swift_name("startHour")));
@property (readonly) NSString *timeZone __attribute__((swift_name("timeZone")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) SharedWWWEventWaveWarming *warming __attribute__((swift_name("warming")));
@property (readonly) SharedWWWEventWave *wave __attribute__((swift_name("wave")));
@property (readonly) SharedWWWEventWWWWaveDefinition *wavedef __attribute__((swift_name("wavedef")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IWWWEventStatus")))
@interface SharedIWWWEventStatus : SharedKotlinEnum<SharedIWWWEventStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedIWWWEventStatus *undefined __attribute__((swift_name("undefined")));
@property (class, readonly) SharedIWWWEventStatus *done __attribute__((swift_name("done")));
@property (class, readonly) SharedIWWWEventStatus *next __attribute__((swift_name("next")));
@property (class, readonly) SharedIWWWEventStatus *soon __attribute__((swift_name("soon")));
@property (class, readonly) SharedIWWWEventStatus *running __attribute__((swift_name("running")));
+ (SharedKotlinArray<SharedIWWWEventStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedIWWWEventStatus *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IWWWEventWaveNumbersLiterals")))
@interface SharedIWWWEventWaveNumbersLiterals : SharedBase
- (instancetype)initWithWaveTimezone:(NSString *)waveTimezone waveSpeed:(NSString *)waveSpeed waveStartTime:(NSString *)waveStartTime waveEndTime:(NSString *)waveEndTime waveTotalTime:(NSString *)waveTotalTime __attribute__((swift_name("init(waveTimezone:waveSpeed:waveStartTime:waveEndTime:waveTotalTime:)"))) __attribute__((objc_designated_initializer));
- (SharedIWWWEventWaveNumbersLiterals *)doCopyWaveTimezone:(NSString *)waveTimezone waveSpeed:(NSString *)waveSpeed waveStartTime:(NSString *)waveStartTime waveEndTime:(NSString *)waveEndTime waveTotalTime:(NSString *)waveTotalTime __attribute__((swift_name("doCopy(waveTimezone:waveSpeed:waveStartTime:waveEndTime:waveTotalTime:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *waveEndTime __attribute__((swift_name("waveEndTime")));
@property (readonly) NSString *waveSpeed __attribute__((swift_name("waveSpeed")));
@property (readonly) NSString *waveStartTime __attribute__((swift_name("waveStartTime")));
@property (readonly) NSString *waveTimezone __attribute__((swift_name("waveTimezone")));
@property (readonly) NSString *waveTotalTime __attribute__((swift_name("waveTotalTime")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEvent")))
@interface SharedWWWEvent : SharedBase <SharedIWWWEvent, SharedDataValidator, SharedKoin_coreKoinComponent>
- (instancetype)initWithId:(NSString *)id type:(NSString *)type country:(NSString * _Nullable)country community:(NSString * _Nullable)community timeZone:(NSString *)timeZone date:(NSString *)date startHour:(NSString *)startHour instagramAccount:(NSString *)instagramAccount instagramHashtag:(NSString *)instagramHashtag wavedef:(SharedWWWEventWWWWaveDefinition *)wavedef area:(SharedWWWEventArea *)area map:(SharedWWWEventMap *)map favorite:(BOOL)favorite __attribute__((swift_name("init(id:type:country:community:timeZone:date:startHour:instagramAccount:instagramHashtag:wavedef:area:map:favorite:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWEventCompanion *companion __attribute__((swift_name("companion")));
- (SharedWWWEvent *)doCopyId:(NSString *)id type:(NSString *)type country:(NSString * _Nullable)country community:(NSString * _Nullable)community timeZone:(NSString *)timeZone date:(NSString *)date startHour:(NSString *)startHour instagramAccount:(NSString *)instagramAccount instagramHashtag:(NSString *)instagramHashtag wavedef:(SharedWWWEventWWWWaveDefinition *)wavedef area:(SharedWWWEventArea *)area map:(SharedWWWEventMap *)map favorite:(BOOL)favorite __attribute__((swift_name("doCopy(id:type:country:community:timeZone:date:startHour:instagramAccount:instagramHashtag:wavedef:area:map:favorite:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getAllNumbersWithCompletionHandler:(void (^)(SharedIWWWEventWaveNumbersLiterals * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getAllNumbers(completionHandler:)")));
- (id _Nullable)getCommunityImage __attribute__((swift_name("getCommunityImage()")));
- (id _Nullable)getCountryImage __attribute__((swift_name("getCountryImage()")));
- (SharedResourcesStringResource *)getDescription __attribute__((swift_name("getDescription()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getEndDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getEndDateTime(completionHandler:)")));
- (SharedWWWEventObserver *)getEventObserver __attribute__((swift_name("getEventObserver()")));
- (SharedResourcesStringResource *)getLiteralCommunity __attribute__((swift_name("getLiteralCommunity()")));
- (SharedResourcesStringResource *)getLiteralCountry __attribute__((swift_name("getLiteralCountry()")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getLiteralEndTimeWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getLiteralEndTime(completionHandler:)")));
- (NSString *)getLiteralStartDateSimple __attribute__((swift_name("getLiteralStartDateSimple()")));
- (NSString *)getLiteralStartTime __attribute__((swift_name("getLiteralStartTime()")));
- (NSString *)getLiteralTimezone __attribute__((swift_name("getLiteralTimezone()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getLiteralTotalTimeWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getLiteralTotalTime(completionHandler:)")));
- (SharedResourcesStringResource *)getLocation __attribute__((swift_name("getLocation()")));
- (id _Nullable)getLocationImage __attribute__((swift_name("getLocationImage()")));
- (id _Nullable)getMapImage __attribute__((swift_name("getMapImage()")));
- (SharedKotlinInstant *)getStartDateTime __attribute__((swift_name("getStartDateTime()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getStatusWithCompletionHandler:(void (^)(SharedIWWWEventStatus * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getStatus(completionHandler:)")));
- (SharedKotlinx_datetimeTimeZone *)getTZ __attribute__((swift_name("getTZ()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTotalTimeWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getTotalTime(completionHandler:)")));
- (int64_t)getWarmingDuration __attribute__((swift_name("getWarmingDuration()")));
- (SharedKotlinInstant *)getWaveStartDateTime __attribute__((swift_name("getWaveStartDateTime()")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isDoneWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isDone(completionHandler:)")));
- (BOOL)isNearTime __attribute__((swift_name("isNearTime()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isRunningWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isRunning(completionHandler:)")));
- (BOOL)isSoon __attribute__((swift_name("isSoon()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) SharedWWWEventArea *area __attribute__((swift_name("area")));

/**
 * @note annotations
 *   kotlinx.serialization.Transient
*/
@property SharedWWWEventObserver * _Nullable cachedObserver __attribute__((swift_name("cachedObserver")));
@property (readonly) NSString * _Nullable community __attribute__((swift_name("community")));
@property (readonly) NSString * _Nullable country __attribute__((swift_name("country")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property BOOL favorite __attribute__((swift_name("favorite")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *instagramAccount __attribute__((swift_name("instagramAccount")));
@property (readonly) NSString *instagramHashtag __attribute__((swift_name("instagramHashtag")));
@property (readonly) SharedWWWEventMap *map __attribute__((swift_name("map")));
@property (readonly) NSString *startHour __attribute__((swift_name("startHour")));
@property (readonly) NSString *timeZone __attribute__((swift_name("timeZone")));
@property (readonly) NSString *type __attribute__((swift_name("type")));

/**
 * @note annotations
 *   kotlinx.serialization.Transient
*/
@property (readonly) SharedWWWEventWaveWarming *warming __attribute__((swift_name("warming")));
@property (readonly) SharedWWWEventWave *wave __attribute__((swift_name("wave")));
@property (readonly) SharedWWWEventWWWWaveDefinition *wavedef __attribute__((swift_name("wavedef")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEvent.Companion")))
@interface SharedWWWEventCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEvent.WWWWaveDefinition")))
@interface SharedWWWEventWWWWaveDefinition : SharedBase <SharedDataValidator>
- (instancetype)initWithLinear:(SharedWWWEventWaveLinear * _Nullable)linear deep:(SharedWWWEventWaveDeep * _Nullable)deep linearSplit:(SharedWWWEventWaveLinearSplit * _Nullable)linearSplit __attribute__((swift_name("init(linear:deep:linearSplit:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWEventWWWWaveDefinitionCompanion *companion __attribute__((swift_name("companion")));
- (SharedWWWEventWWWWaveDefinition *)doCopyLinear:(SharedWWWEventWaveLinear * _Nullable)linear deep:(SharedWWWEventWaveDeep * _Nullable)deep linearSplit:(SharedWWWEventWaveLinearSplit * _Nullable)linearSplit __attribute__((swift_name("doCopy(linear:deep:linearSplit:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) SharedWWWEventWaveDeep * _Nullable deep __attribute__((swift_name("deep")));
@property (readonly) SharedWWWEventWaveLinear * _Nullable linear __attribute__((swift_name("linear")));
@property (readonly) SharedWWWEventWaveLinearSplit * _Nullable linearSplit __attribute__((swift_name("linearSplit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEvent.WWWWaveDefinitionCompanion")))
@interface SharedWWWEventWWWWaveDefinitionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventWWWWaveDefinitionCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventArea")))
@interface SharedWWWEventArea : SharedBase <SharedKoin_coreKoinComponent, SharedDataValidator>
- (instancetype)initWithOsmAdminids:(NSArray<SharedInt *> *)osmAdminids bbox:(NSString * _Nullable)bbox __attribute__((swift_name("init(osmAdminids:bbox:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWEventAreaCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)bboxWithCompletionHandler:(void (^)(SharedBoundingBox * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("bbox(completionHandler:)")));
- (void)clearCache __attribute__((swift_name("clearCache()")));
- (SharedWWWEventArea *)doCopyOsmAdminids:(NSArray<SharedInt *> *)osmAdminids bbox:(NSString * _Nullable)bbox __attribute__((swift_name("doCopy(osmAdminids:bbox:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)generateRandomPositionInAreaWithCompletionHandler:(void (^)(SharedPosition * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("generateRandomPositionInArea(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCenterWithCompletionHandler:(void (^)(SharedPosition * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getCenter(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getPolygonsWithCompletionHandler:(void (^)(NSArray<SharedPolygon *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getPolygons(completionHandler:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isPositionWithinPosition:(SharedPosition *)position completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isPositionWithin(position:completionHandler:)")));
- (void)setRelatedEventEvent:(SharedWWWEvent *)event __attribute__((swift_name("setRelatedEvent(event:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) NSString * _Nullable bbox __attribute__((swift_name("bbox")));
@property (readonly) BOOL bboxIsOverride __attribute__((swift_name("bboxIsOverride")));
@property (readonly) NSArray<SharedInt *> *osmAdminids __attribute__((swift_name("osmAdminids")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> polygonsLoaded __attribute__((swift_name("polygonsLoaded")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventArea.Companion")))
@interface SharedWWWEventAreaCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventAreaCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventMap")))
@interface SharedWWWEventMap : SharedBase <SharedKoin_coreKoinComponent, SharedDataValidator>
- (instancetype)initWithMaxZoom:(double)maxZoom language:(NSString *)language zone:(NSString *)zone __attribute__((swift_name("init(maxZoom:language:zone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWEventMapCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cacheSpriteAndGlyphsWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("cacheSpriteAndGlyphs(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getStyleUriWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getStyleUri(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isPositionWithinPosition:(SharedPosition *)position completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isPositionWithin(position:completionHandler:)")));
- (void)setRelatedEventEvent:(SharedWWWEvent *)event __attribute__((swift_name("setRelatedEvent(event:)")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) NSString *language __attribute__((swift_name("language")));
@property (readonly) double maxZoom __attribute__((swift_name("maxZoom")));
@property (readonly) NSString *zone __attribute__((swift_name("zone")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventMap.Companion")))
@interface SharedWWWEventMapCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventMapCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventObserver")))
@interface SharedWWWEventObserver : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)initWithEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("init(event:)"))) __attribute__((objc_designated_initializer));
- (void)startObservation __attribute__((swift_name("startObservation()")));
- (void)stopObservation __attribute__((swift_name("stopObservation()")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)validateStateConsistencyWithCompletionHandler:(void (^)(NSArray<NSString *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("validateStateConsistency(completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> eventStatus __attribute__((swift_name("eventStatus")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> hitDateTime __attribute__((swift_name("hitDateTime")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isStartWarmingInProgress __attribute__((swift_name("isStartWarmingInProgress")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isUserWarmingInProgress __attribute__((swift_name("isUserWarmingInProgress")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> progression __attribute__((swift_name("progression")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> timeBeforeHit __attribute__((swift_name("timeBeforeHit")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> userHasBeenHit __attribute__((swift_name("userHasBeenHit")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> userIsGoingToBeHit __attribute__((swift_name("userIsGoingToBeHit")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> userIsInArea __attribute__((swift_name("userIsInArea")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> userPositionRatio __attribute__((swift_name("userPositionRatio")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventObserver.EventObservation")))
@interface SharedWWWEventObserverEventObservation : SharedBase
- (instancetype)initWithProgression:(double)progression status:(SharedIWWWEventStatus *)status __attribute__((swift_name("init(progression:status:)"))) __attribute__((objc_designated_initializer));
- (SharedWWWEventObserverEventObservation *)doCopyProgression:(double)progression status:(SharedIWWWEventStatus *)status __attribute__((swift_name("doCopy(progression:status:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double progression __attribute__((swift_name("progression")));
@property (readonly) SharedIWWWEventStatus *status __attribute__((swift_name("status")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((swift_name("WWWEventWave")))
@interface SharedWWWEventWave : SharedBase <SharedKoin_coreKoinComponent, SharedDataValidator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithSeen0:(int32_t)seen0 serializationConstructorMarker:(id _Nullable)serializationConstructorMarker __attribute__((swift_name("init(seen0:serializationConstructorMarker:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedWWWEventWaveCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)bboxWithCompletionHandler:(void (^)(SharedBoundingBox * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("bbox(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)closestWaveLongitudeLatitude:(double)latitude completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("closestWaveLongitude(latitude:completionHandler:)")));
- (int64_t)getApproxDuration __attribute__((swift_name("getApproxDuration()")));
- (NSString *)getLiteralFromProgressionProgression:(double)progression __attribute__((swift_name("getLiteralFromProgression(progression:)")));
- (NSString *)getLiteralSpeed __attribute__((swift_name("getLiteralSpeed()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getProgressionWithCompletionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getProgression(completionHandler:)")));
- (SharedPosition * _Nullable)getUserPosition __attribute__((swift_name("getUserPosition()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWaveDurationWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWaveDuration(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWavePolygonsWithCompletionHandler:(void (^)(SharedWWWEventWaveWavePolygons * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getWavePolygons(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasUserBeenHitInCurrentPositionWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasUserBeenHitInCurrentPosition(completionHandler:)")));
- (SharedChoreographyManagerDisplayableSequence<SharedLibraryDrawableResource *> * _Nullable)hitChoregraphySequence __attribute__((swift_name("hitChoregraphySequence()")));
- (void)notifyPositionChangedPosition:(SharedPosition * _Nullable)position __attribute__((swift_name("notifyPositionChanged(position:)")));
- (SharedWWWEventWave *)setPositionRequesterPositionRequester:(SharedPosition * _Nullable (^)(void))positionRequester __attribute__((swift_name("setPositionRequester(positionRequester:)")));
- (SharedWWWEventWave *)setRelatedEventEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("setRelatedEvent(event:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)timeBeforeUserHitWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("timeBeforeUserHit(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userClosestWaveLongitudeWithCompletionHandler:(void (^)(SharedDouble * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userClosestWaveLongitude(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userHitDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userHitDateTime(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userPositionToWaveRatioWithCompletionHandler:(void (^)(SharedDouble * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userPositionToWaveRatio(completionHandler:)")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
- (SharedChoreographyManagerDisplayableSequence<SharedLibraryDrawableResource *> * _Nullable)waitingChoregraphySequence __attribute__((swift_name("waitingChoregraphySequence()")));
@property (readonly) int32_t approxDuration __attribute__((swift_name("approxDuration")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SharedIClock> clock __attribute__((swift_name("clock")));
@property (readonly) SharedWWWEventWaveDirection *direction __attribute__((swift_name("direction")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SharedIWWWEvent> event __attribute__((swift_name("event")));

/**
 * @note annotations
 *   kotlinx.serialization.Transient
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property SharedPosition * _Nullable (^ _Nullable positionRequester)(void) __attribute__((swift_name("positionRequester")));

/**
 * @note annotations
 *   kotlinx.serialization.Transient
*/
@property (readonly) id<SharedKotlinx_coroutines_coreSharedFlow> positionUpdates __attribute__((swift_name("positionUpdates")));
@property (readonly) double speed __attribute__((swift_name("speed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWave.Companion")))
@interface SharedWWWEventWaveCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventWaveCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWave.Direction")))
@interface SharedWWWEventWaveDirection : SharedKotlinEnum<SharedWWWEventWaveDirection *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedWWWEventWaveDirection *west __attribute__((swift_name("west")));
@property (class, readonly) SharedWWWEventWaveDirection *east __attribute__((swift_name("east")));
+ (SharedKotlinArray<SharedWWWEventWaveDirection *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedWWWEventWaveDirection *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWave.WavePolygons")))
@interface SharedWWWEventWaveWavePolygons : SharedBase
- (instancetype)initWithTimestamp:(SharedKotlinInstant *)timestamp traversedPolygons:(NSArray<SharedPolygon *> *)traversedPolygons remainingPolygons:(NSArray<SharedPolygon *> *)remainingPolygons __attribute__((swift_name("init(timestamp:traversedPolygons:remainingPolygons:)"))) __attribute__((objc_designated_initializer));
- (SharedWWWEventWaveWavePolygons *)doCopyTimestamp:(SharedKotlinInstant *)timestamp traversedPolygons:(NSArray<SharedPolygon *> *)traversedPolygons remainingPolygons:(NSArray<SharedPolygon *> *)remainingPolygons __attribute__((swift_name("doCopy(timestamp:traversedPolygons:remainingPolygons:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedPolygon *> *remainingPolygons __attribute__((swift_name("remainingPolygons")));
@property (readonly) SharedKotlinInstant *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) NSArray<SharedPolygon *> *traversedPolygons __attribute__((swift_name("traversedPolygons")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveDeep")))
@interface SharedWWWEventWaveDeep : SharedWWWEventWave <SharedKoin_coreKoinComponent>
- (instancetype)initWithSpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration __attribute__((swift_name("init(speed:direction:approxDuration:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithSeen0:(int32_t)seen0 serializationConstructorMarker:(id _Nullable)serializationConstructorMarker __attribute__((swift_name("init(seen0:serializationConstructorMarker:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedWWWEventWaveDeepCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)closestWaveLongitudeLatitude:(double)latitude completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("closestWaveLongitude(latitude:completionHandler:)")));
- (SharedWWWEventWaveDeep *)doCopySpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration __attribute__((swift_name("doCopy(speed:direction:approxDuration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWaveDurationWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWaveDuration(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWavePolygonsWithCompletionHandler:(void (^)(SharedWWWEventWaveWavePolygons * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWavePolygons(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasUserBeenHitInCurrentPositionWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasUserBeenHitInCurrentPosition(completionHandler:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userHitDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userHitDateTime(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userPositionToWaveRatioWithCompletionHandler:(void (^)(SharedDouble * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userPositionToWaveRatio(completionHandler:)")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) int32_t approxDuration __attribute__((swift_name("approxDuration")));
@property (readonly) SharedWWWEventWaveDirection *direction __attribute__((swift_name("direction")));
@property (readonly) double speed __attribute__((swift_name("speed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveDeep.Companion")))
@interface SharedWWWEventWaveDeepCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventWaveDeepCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveLinear")))
@interface SharedWWWEventWaveLinear : SharedWWWEventWave <SharedKoin_coreKoinComponent>
- (instancetype)initWithSpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration __attribute__((swift_name("init(speed:direction:approxDuration:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithSeen0:(int32_t)seen0 serializationConstructorMarker:(id _Nullable)serializationConstructorMarker __attribute__((swift_name("init(seen0:serializationConstructorMarker:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedWWWEventWaveLinearCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)closestWaveLongitudeLatitude:(double)latitude completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("closestWaveLongitude(latitude:completionHandler:)")));
- (SharedWWWEventWaveLinear *)doCopySpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration __attribute__((swift_name("doCopy(speed:direction:approxDuration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWaveDurationWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWaveDuration(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWavePolygonsWithCompletionHandler:(void (^)(SharedWWWEventWaveWavePolygons * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getWavePolygons(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasUserBeenHitInCurrentPositionWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasUserBeenHitInCurrentPosition(completionHandler:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userHitDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userHitDateTime(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userPositionToWaveRatioWithCompletionHandler:(void (^)(SharedDouble * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userPositionToWaveRatio(completionHandler:)")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) int32_t approxDuration __attribute__((swift_name("approxDuration")));
@property (readonly) SharedWWWEventWaveDirection *direction __attribute__((swift_name("direction")));
@property (readonly) double speed __attribute__((swift_name("speed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveLinear.Companion")))
@interface SharedWWWEventWaveLinearCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventWaveLinearCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveLinearSplit")))
@interface SharedWWWEventWaveLinearSplit : SharedWWWEventWave <SharedKoin_coreKoinComponent>
- (instancetype)initWithSpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration nbSplits:(int32_t)nbSplits __attribute__((swift_name("init(speed:direction:approxDuration:nbSplits:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithSeen0:(int32_t)seen0 serializationConstructorMarker:(id _Nullable)serializationConstructorMarker __attribute__((swift_name("init(seen0:serializationConstructorMarker:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedWWWEventWaveLinearSplitCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)closestWaveLongitudeLatitude:(double)latitude completionHandler:(void (^)(SharedDouble * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("closestWaveLongitude(latitude:completionHandler:)")));
- (SharedWWWEventWaveLinearSplit *)doCopySpeed:(double)speed direction:(SharedWWWEventWaveDirection *)direction approxDuration:(int32_t)approxDuration nbSplits:(int32_t)nbSplits __attribute__((swift_name("doCopy(speed:direction:approxDuration:nbSplits:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWaveDurationWithCompletionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWaveDuration(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getWavePolygonsWithCompletionHandler:(void (^)(SharedWWWEventWaveWavePolygons * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getWavePolygons(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasUserBeenHitInCurrentPositionWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasUserBeenHitInCurrentPosition(completionHandler:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userHitDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userHitDateTime(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userPositionToWaveRatioWithCompletionHandler:(void (^)(SharedDouble * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userPositionToWaveRatio(completionHandler:)")));
- (NSArray<NSString *> * _Nullable)validationErrors __attribute__((swift_name("validationErrors()")));
@property (readonly) int32_t approxDuration __attribute__((swift_name("approxDuration")));
@property (readonly) SharedWWWEventWaveDirection *direction __attribute__((swift_name("direction")));
@property (readonly) int32_t nbSplits __attribute__((swift_name("nbSplits")));
@property (readonly) double speed __attribute__((swift_name("speed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveLinearSplit.Companion")))
@interface SharedWWWEventWaveLinearSplitCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWEventWaveLinearSplitCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEventWaveWarming")))
@interface SharedWWWEventWaveWarming : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)initWithEvent:(id<SharedIWWWEvent>)event __attribute__((swift_name("init(event:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCurrentChoregraphySequenceWithCompletionHandler:(void (^)(SharedChoreographyManagerDisplayableSequence<SharedLibraryDrawableResource *> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getCurrentChoregraphySequence(completionHandler:)")));
- (int64_t)getWarmingDuration __attribute__((swift_name("getWarmingDuration()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isUserWarmingStartedWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isUserWarmingStarted(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)playCurrentSoundChoreographyToneWithCompletionHandler:(void (^)(SharedInt * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("playCurrentSoundChoreographyTone(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)playCurrentSoundChoreographyToneForceStartTime:(SharedKotlinInstant * _Nullable)forceStartTime completionHandler:(void (^)(SharedInt * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("playCurrentSoundChoreographyTone(forceStartTime:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)userWarmingStartDateTimeWithCompletionHandler:(void (^)(SharedKotlinInstant * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("userWarmingStartDateTime(completionHandler:)")));
@property (readonly) id<SharedIWWWEvent> event __attribute__((swift_name("event")));
@property (readonly) SharedSoundChoreographyManager *soundChoreographyManager __attribute__((swift_name("soundChoreographyManager")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWEvents")))
@interface SharedWWWEvents : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addOnEventsErrorListenerCallback:(void (^)(SharedKotlinException *))callback __attribute__((swift_name("addOnEventsErrorListener(callback:)")));
- (void)addOnEventsLoadedListenerCallback:(void (^)(void))callback __attribute__((swift_name("addOnEventsLoadedListener(callback:)")));
- (void)addOnTerminationListenerCallback:(void (^)(SharedKotlinException * _Nullable))callback __attribute__((swift_name("addOnTerminationListener(callback:)")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (NSDictionary<id<SharedIWWWEvent>, id> *)confValidationErrorsEvents:(NSArray<id<SharedIWWWEvent>> *)events __attribute__((swift_name("confValidationErrors(events:)")));
- (id<SharedKotlinx_coroutines_coreStateFlow>)flow __attribute__((swift_name("flow()")));
- (id<SharedIWWWEvent> _Nullable)getEventByIdId:(NSString *)id __attribute__((swift_name("getEventById(id:)")));
- (SharedKotlinException * _Nullable)getLoadingError __attribute__((swift_name("getLoadingError()")));
- (NSArray<SharedKotlinPair<id<SharedIWWWEvent>, NSArray<NSString *> *> *> *)getValidationErrors __attribute__((swift_name("getValidationErrors()")));
- (BOOL)isLoaded __attribute__((swift_name("isLoaded()")));
- (NSArray<id<SharedIWWWEvent>> *)list __attribute__((swift_name("list()")));

/**
 * @note annotations
 *   kotlin.jvm.JvmOverloads
*/
- (SharedWWWEvents *)loadEventsOnLoaded:(void (^ _Nullable)(void))onLoaded onLoadingError:(void (^ _Nullable)(SharedKotlinException *))onLoadingError onTermination:(void (^ _Nullable)(SharedKotlinException * _Nullable))onTermination __attribute__((swift_name("loadEvents(onLoaded:onLoadingError:onTermination:)")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (void)onEventsLoaded __attribute__((swift_name("onEventsLoaded()")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (void)onLoadingErrorException:(SharedKotlinException *)exception __attribute__((swift_name("onLoadingError(exception:)")));
- (void)restartObserversOnSimulationChange __attribute__((swift_name("restartObserversOnSimulationChange()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BoundingBox")))
@interface SharedBoundingBox : SharedBase
- (instancetype)initWithSwLat:(double)swLat swLng:(double)swLng neLat:(double)neLat neLng:(double)neLng __attribute__((swift_name("init(swLat:swLng:neLat:neLng:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedBoundingBoxCompanion *companion __attribute__((swift_name("companion")));
- (SharedPosition *)component1 __attribute__((swift_name("component1()")));
- (SharedPosition *)component2 __attribute__((swift_name("component2()")));
- (BOOL)containsPosition:(SharedPosition *)position __attribute__((swift_name("contains(position:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedBoundingBox *)expandFactor:(double)factor __attribute__((swift_name("expand(factor:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)intersectsOther:(SharedBoundingBox *)other __attribute__((swift_name("intersects(other:)")));
- (double)latitudeOfWidestPart __attribute__((swift_name("latitudeOfWidestPart()")));
@property (readonly) double eastLongitude __attribute__((swift_name("eastLongitude")));
@property (readonly) double height __attribute__((swift_name("height")));
@property (readonly) double maxLatitude __attribute__((swift_name("maxLatitude")));
@property (readonly) double maxLongitude __attribute__((swift_name("maxLongitude")));
@property (readonly) double minLatitude __attribute__((swift_name("minLatitude")));
@property (readonly) double minLongitude __attribute__((swift_name("minLongitude")));
@property (readonly) SharedPosition *ne __attribute__((swift_name("ne")));
@property (readonly) double northLatitude __attribute__((swift_name("northLatitude")));
@property (readonly) SharedPosition *northeast __attribute__((swift_name("northeast")));
@property (readonly) double southLatitude __attribute__((swift_name("southLatitude")));
@property (readonly) SharedPosition *southwest __attribute__((swift_name("southwest")));
@property (readonly) SharedPosition *sw __attribute__((swift_name("sw")));
@property (readonly) double westLongitude __attribute__((swift_name("westLongitude")));
@property (readonly) double width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BoundingBox.Companion")))
@interface SharedBoundingBoxCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBoundingBoxCompanion *shared __attribute__((swift_name("shared")));
- (SharedBoundingBox * _Nullable)fromCornersPositions:(NSArray<SharedPosition *> *)positions __attribute__((swift_name("fromCorners(positions:)")));
- (SharedBoundingBox *)fromCornersSw:(SharedPosition *)sw ne:(SharedPosition *)ne __attribute__((swift_name("fromCorners(sw:ne:)")));
@end

__attribute__((swift_name("KotlinIterable")))
@protocol SharedKotlinIterable
@required
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end

__attribute__((swift_name("ComposedLongitude")))
@interface SharedComposedLongitude : SharedBase <SharedKotlinIterable>
- (instancetype)initWithPosition:(SharedPosition * _Nullable)position __attribute__((swift_name("init(position:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedComposedLongitudeCompanion *companion __attribute__((swift_name("companion")));
- (void)addPosition:(SharedPosition *)position __attribute__((swift_name("add(position:)")));
- (void)addAllNewPositions:(NSArray<SharedPosition *> *)newPositions __attribute__((swift_name("addAll(newPositions:)")));
- (SharedBoundingBox *)bbox __attribute__((swift_name("bbox()")));
- (SharedComposedLongitude *)clear __attribute__((swift_name("clear()")));
- (NSArray<SharedPosition *> *)getPositions __attribute__((swift_name("getPositions()")));
- (SharedPosition * _Nullable)intersectWithSegmentSegment:(SharedSegment *)segment __attribute__((swift_name("intersectWithSegment(segment:)")));
- (SharedCutPosition * _Nullable)intersectWithSegmentCutId:(int32_t)cutId segment:(SharedSegment *)segment __attribute__((swift_name("intersectWithSegment(cutId:segment:)")));
- (SharedComposedLongitudeSide *)isPointOnLinePoint:(SharedPosition *)point __attribute__((swift_name("isPointOnLine(point:)")));
- (BOOL)isValidArcPositions:(NSArray<SharedPosition *> *)positions __attribute__((swift_name("isValidArc(positions:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (SharedDouble * _Nullable)lngAtLat:(double)lat __attribute__((swift_name("lngAt(lat:)")));
- (NSArray<SharedPosition *> *)positionsBetweenMinLat:(double)minLat maxLat:(double)maxLat __attribute__((swift_name("positionsBetween(minLat:maxLat:)")));
- (id<SharedKotlinIterator>)reverseIterator __attribute__((swift_name("reverseIterator()")));
- (int32_t)size __attribute__((swift_name("size()")));
@property (readonly) SharedComposedLongitudeOrientation *orientation __attribute__((swift_name("orientation")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ComposedLongitude.Companion")))
@interface SharedComposedLongitudeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedComposedLongitudeCompanion *shared __attribute__((swift_name("shared")));
- (SharedComposedLongitude *)fromLongitudeLongitude:(double)longitude __attribute__((swift_name("fromLongitude(longitude:)")));
- (SharedComposedLongitude *)fromPositionsPositions:(SharedKotlinArray<SharedPosition *> *)positions __attribute__((swift_name("fromPositions(positions:)")));
- (SharedComposedLongitude *)fromPositionsPositions_:(NSArray<SharedPosition *> *)positions __attribute__((swift_name("fromPositions(positions_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ComposedLongitude.Orientation")))
@interface SharedComposedLongitudeOrientation : SharedKotlinEnum<SharedComposedLongitudeOrientation *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedComposedLongitudeOrientation *north __attribute__((swift_name("north")));
@property (class, readonly) SharedComposedLongitudeOrientation *south __attribute__((swift_name("south")));
+ (SharedKotlinArray<SharedComposedLongitudeOrientation *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedComposedLongitudeOrientation *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ComposedLongitude.Side")))
@interface SharedComposedLongitudeSide : SharedKotlinEnum<SharedComposedLongitudeSide *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedComposedLongitudeSide *east __attribute__((swift_name("east")));
@property (class, readonly) SharedComposedLongitudeSide *west __attribute__((swift_name("west")));
@property (class, readonly) SharedComposedLongitudeSide *on __attribute__((swift_name("on")));
+ (SharedKotlinArray<SharedComposedLongitudeSide *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedComposedLongitudeSide *> *entries __attribute__((swift_name("entries")));
- (BOOL)isEast __attribute__((swift_name("isEast()")));
- (BOOL)isOn __attribute__((swift_name("isOn()")));
- (BOOL)isWest __attribute__((swift_name("isWest()")));
@end

__attribute__((swift_name("CoroutineScopeProvider")))
@protocol SharedCoroutineScopeProvider
@required
- (void)cancelAllCoroutines __attribute__((swift_name("cancelAllCoroutines()")));
- (id<SharedKotlinx_coroutines_coreJob>)launchDefaultBlock:(id<SharedKotlinSuspendFunction1>)block __attribute__((swift_name("launchDefault(block:)")));
- (id<SharedKotlinx_coroutines_coreJob>)launchIOBlock:(id<SharedKotlinSuspendFunction1>)block __attribute__((swift_name("launchIO(block:)")));
- (id<SharedKotlinx_coroutines_coreCoroutineScope>)scopeDefault __attribute__((swift_name("scopeDefault()")));
- (id<SharedKotlinx_coroutines_coreCoroutineScope>)scopeIO __attribute__((swift_name("scopeIO()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)withDefaultContextBlock:(id<SharedKotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withDefaultContext(block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)withIOContextBlock:(id<SharedKotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withIOContext(block:completionHandler:)")));
@end

__attribute__((swift_name("Position")))
@interface SharedPosition : SharedBase
- (instancetype)initWithLat:(double)lat lng:(double)lng prev:(SharedPosition * _Nullable)prev next:(SharedPosition * _Nullable)next __attribute__((swift_name("init(lat:lng:prev:next:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedPositionCompanion *companion __attribute__((swift_name("companion")));
- (double)component1 __attribute__((swift_name("component1()")));
- (double)component2 __attribute__((swift_name("component2()")));
- (SharedPosition *)doCopyLat:(SharedDouble * _Nullable)lat lng:(SharedDouble * _Nullable)lng __attribute__((swift_name("doCopy(lat:lng:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedCutPosition *)toCutPositionCutId:(int32_t)cutId cutLeft:(SharedPosition *)cutLeft cutRight:(SharedPosition *)cutRight __attribute__((swift_name("toCutPosition(cutId:cutLeft:cutRight:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t id_ __attribute__((swift_name("id_")));
@property (readonly) double lat __attribute__((swift_name("lat")));
@property (readonly) double latitude __attribute__((swift_name("latitude")));
@property (readonly) double lng __attribute__((swift_name("lng")));
@property (readonly) double longitude __attribute__((swift_name("longitude")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CutPosition")))
@interface SharedCutPosition : SharedPosition
- (instancetype)initWithLat:(double)lat lng:(double)lng cutId:(int32_t)cutId cutLeft:(SharedPosition *)cutLeft cutRight:(SharedPosition *)cutRight __attribute__((swift_name("init(lat:lng:cutId:cutLeft:cutRight:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithLat:(double)lat lng:(double)lng prev:(SharedPosition * _Nullable)prev next:(SharedPosition * _Nullable)next __attribute__((swift_name("init(lat:lng:prev:next:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
@property (readonly) int32_t cutId __attribute__((swift_name("cutId")));
@property (readonly) SharedPosition *cutLeft __attribute__((swift_name("cutLeft")));
@property (readonly) SharedPosition *cutRight __attribute__((swift_name("cutRight")));
@property (readonly) double pairId __attribute__((swift_name("pairId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultCoroutineScopeProvider")))
@interface SharedDefaultCoroutineScopeProvider : SharedBase <SharedCoroutineScopeProvider>
- (instancetype)initWithIoDispatcher:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)ioDispatcher defaultDispatcher:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)defaultDispatcher __attribute__((swift_name("init(ioDispatcher:defaultDispatcher:)"))) __attribute__((objc_designated_initializer));
- (void)cancelAllCoroutines __attribute__((swift_name("cancelAllCoroutines()")));
- (id<SharedKotlinx_coroutines_coreJob>)launchDefaultBlock:(id<SharedKotlinSuspendFunction1>)block __attribute__((swift_name("launchDefault(block:)")));
- (id<SharedKotlinx_coroutines_coreJob>)launchIOBlock:(id<SharedKotlinSuspendFunction1>)block __attribute__((swift_name("launchIO(block:)")));
- (id<SharedKotlinx_coroutines_coreCoroutineScope>)scopeDefault __attribute__((swift_name("scopeDefault()")));
- (id<SharedKotlinx_coroutines_coreCoroutineScope>)scopeIO __attribute__((swift_name("scopeIO()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)withDefaultContextBlock:(id<SharedKotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withDefaultContext(block:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)withIOContextBlock:(id<SharedKotlinSuspendFunction1>)block completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("withIOContext(block:completionHandler:)")));
@end

__attribute__((swift_name("EventsConfigurationProvider")))
@protocol SharedEventsConfigurationProvider
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)geoEventsConfigurationWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("geoEventsConfiguration(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultEventsConfigurationProvider")))
@interface SharedDefaultEventsConfigurationProvider : SharedBase <SharedEventsConfigurationProvider>
- (instancetype)initWithCoroutineScopeProvider:(id<SharedCoroutineScopeProvider>)coroutineScopeProvider __attribute__((swift_name("init(coroutineScopeProvider:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)geoEventsConfigurationWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("geoEventsConfiguration(completionHandler:)")));
@end

__attribute__((swift_name("EventsDecoder")))
@protocol SharedEventsDecoder
@required
- (NSArray<id<SharedIWWWEvent>> *)decodeFromJsonJsonString:(NSString *)jsonString __attribute__((swift_name("decodeFromJson(jsonString:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultEventsDecoder")))
@interface SharedDefaultEventsDecoder : SharedBase <SharedEventsDecoder>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<SharedWWWEvent *> *)decodeFromJsonJsonString:(NSString *)jsonString __attribute__((swift_name("decodeFromJson(jsonString:)")));
@end

__attribute__((swift_name("GeoJsonDataProvider")))
@protocol SharedGeoJsonDataProvider
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getGeoJsonDataEventId:(NSString *)eventId completionHandler:(void (^)(NSDictionary<NSString *, SharedKotlinx_serialization_jsonJsonElement *> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getGeoJsonData(eventId:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultGeoJsonDataProvider")))
@interface SharedDefaultGeoJsonDataProvider : SharedBase <SharedGeoJsonDataProvider>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getGeoJsonDataEventId:(NSString *)eventId completionHandler:(void (^)(NSDictionary<NSString *, SharedKotlinx_serialization_jsonJsonElement *> * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getGeoJsonData(eventId:completionHandler:)")));
@end

__attribute__((swift_name("MapDataProvider")))
@protocol SharedMapDataProvider
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)geoMapStyleDataWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("geoMapStyleData(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultMapDataProvider")))
@interface SharedDefaultMapDataProvider : SharedBase <SharedMapDataProvider>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)geoMapStyleDataWithCompletionHandler:(void (^)(NSString * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("geoMapStyleData(completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EarthAdaptedSpeedLongitude")))
@interface SharedEarthAdaptedSpeedLongitude : SharedComposedLongitude
- (instancetype)initWithCoveredArea:(SharedBoundingBox *)coveredArea speed:(double)speed direction:(SharedWWWEventWaveDirection *)direction __attribute__((swift_name("init(coveredArea:speed:direction:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPosition:(SharedPosition * _Nullable)position __attribute__((swift_name("init(position:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (double)adjustLongitudeWidthAtLatitudeLatitude:(double)latitude lonWidthAtTheLongest:(double)lonWidthAtTheLongest __attribute__((swift_name("adjustLongitudeWidthAtLatitude(latitude:lonWidthAtTheLongest:)")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (NSDictionary<SharedDouble *, SharedEarthAdaptedSpeedLongitudeLatLonBand *> *)bands __attribute__((swift_name("bands()")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (double)calculateLonBandWidthAtLatitudeLatitude:(double)latitude __attribute__((swift_name("calculateLonBandWidthAtLatitude(latitude:)")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (double)calculateOptimalLatBandWidthLatitude:(double)latitude lonBandWidthAtEquator:(double)lonBandWidthAtEquator __attribute__((swift_name("calculateOptimalLatBandWidth(latitude:lonBandWidthAtEquator:)")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
- (NSArray<SharedEarthAdaptedSpeedLongitudeLatLonBand *> *)calculateWaveBands __attribute__((swift_name("calculateWaveBands()")));
- (SharedComposedLongitude *)withProgressionElapsedTime:(int64_t)elapsedTime __attribute__((swift_name("withProgression(elapsedTime:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EarthAdaptedSpeedLongitude.LatLonBand")))
@interface SharedEarthAdaptedSpeedLongitudeLatLonBand : SharedBase
- (instancetype)initWithLatitude:(double)latitude latWidth:(double)latWidth lngWidth:(double)lngWidth __attribute__((swift_name("init(latitude:latWidth:lngWidth:)"))) __attribute__((objc_designated_initializer));
- (SharedEarthAdaptedSpeedLongitudeLatLonBand *)doCopyLatitude:(double)latitude latWidth:(double)latWidth lngWidth:(double)lngWidth __attribute__((swift_name("doCopy(latitude:latWidth:lngWidth:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double latWidth __attribute__((swift_name("latWidth")));
@property (readonly) double latitude __attribute__((swift_name("latitude")));
@property (readonly) double lngWidth __attribute__((swift_name("lngWidth")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GeoUtils")))
@interface SharedGeoUtils : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)geoUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedGeoUtils *shared __attribute__((swift_name("shared")));
- (double)calculateDistanceLonWidth:(double)lonWidth lat:(double)lat __attribute__((swift_name("calculateDistance(lonWidth:lat:)")));
- (double)calculateDistanceLon1:(double)lon1 lon2:(double)lon2 lat:(double)lat __attribute__((swift_name("calculateDistance(lon1:lon2:lat:)")));
- (double)calculateDistanceAccurateLon1:(double)lon1 lon2:(double)lon2 lat:(double)lat __attribute__((swift_name("calculateDistanceAccurate(lon1:lon2:lat:)")));
- (double)calculateDistanceFastLon1:(double)lon1 lon2:(double)lon2 lat:(double)lat __attribute__((swift_name("calculateDistanceFast(lon1:lon2:lat:)")));
- (void)clearTrignometricCaches __attribute__((swift_name("clearTrignometricCaches()")));
- (BOOL)isLatitudeInRangeLat:(double)lat start:(double)start end:(double)end __attribute__((swift_name("isLatitudeInRange(lat:start:end:)")));
- (BOOL)isLongitudeEqualLng1:(double)lng1 lng2:(double)lng2 __attribute__((swift_name("isLongitudeEqual(lng1:lng2:)")));
- (BOOL)isLongitudeInRangeLng:(double)lng start:(double)start end:(double)end __attribute__((swift_name("isLongitudeInRange(lng:start:end:)")));
- (BOOL)isPointOnSegmentPoint:(SharedPosition *)point segment:(SharedSegment *)segment __attribute__((swift_name("isPointOnSegment(point:segment:)")));
- (double)toDegrees:(double)receiver __attribute__((swift_name("toDegrees(_:)")));
- (double)toRadians:(double)receiver __attribute__((swift_name("toRadians(_:)")));
@property (readonly) double EARTH_RADIUS __attribute__((swift_name("EARTH_RADIUS")));
@property (readonly) double EPSILON __attribute__((swift_name("EPSILON")));
@property (readonly) double MIN_PERCEPTIBLE_SPEED_DIFFERENCE __attribute__((swift_name("MIN_PERCEPTIBLE_SPEED_DIFFERENCE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GeoUtils.Vector2D")))
@interface SharedGeoUtilsVector2D : SharedBase
- (instancetype)initWithX:(double)x y:(double)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
- (SharedGeoUtilsVector2D *)doCopyX:(double)x y:(double)y __attribute__((swift_name("doCopy(x:y:)")));
- (double)crossOther:(SharedGeoUtilsVector2D *)other __attribute__((swift_name("cross(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double x __attribute__((swift_name("x")));
@property (readonly) double y __attribute__((swift_name("y")));
@end

__attribute__((swift_name("IClock")))
@protocol SharedIClock
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)delayDuration:(int64_t)duration completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(duration:completionHandler:)")));
- (SharedKotlinInstant *)now __attribute__((swift_name("now()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IClockCompanion")))
@interface SharedIClockCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedIClockCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)instantToLiteralInstant:(SharedKotlinInstant *)instant timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("instantToLiteral(instant:timeZone:)")));
@end

__attribute__((swift_name("Polygon")))
@interface SharedPolygon : SharedBase <SharedKotlinIterable>
- (instancetype)initWithPosition:(SharedPosition * _Nullable)position __attribute__((swift_name("init(position:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedPolygonCompanion *companion __attribute__((swift_name("companion")));
- (SharedPosition *)addPosition:(SharedPosition *)position __attribute__((swift_name("add(position:)")));
- (void)addAllPolygon:(SharedPolygon *)polygon __attribute__((swift_name("addAll(polygon:)")));
- (SharedBoundingBox *)bbox __attribute__((swift_name("bbox()")));
- (SharedPolygon *)clear __attribute__((swift_name("clear()")));
- (SharedPolygon *)createNew __attribute__((swift_name("createNew()")));
- (id<SharedKotlinMutableIterator>)cutIterator __attribute__((swift_name("cutIterator()")));
- (SharedPosition * _Nullable)first __attribute__((swift_name("first()")));
- (void)forceDirectionComputation __attribute__((swift_name("forceDirectionComputation()")));
- (NSSet<SharedCutPosition *> *)getCutPositions __attribute__((swift_name("getCutPositions()")));
- (SharedPosition *)insertAfterNewPosition:(SharedPosition *)newPosition id:(int32_t)id __attribute__((swift_name("insertAfter(newPosition:id:)")));
- (SharedPosition *)insertBeforeNewPosition:(SharedPosition *)newPosition id:(int32_t)id __attribute__((swift_name("insertBefore(newPosition:id:)")));
- (BOOL)isClockwise __attribute__((swift_name("isClockwise()")));
- (BOOL)isCutEmpty __attribute__((swift_name("isCutEmpty()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (BOOL)isNotCutEmpty __attribute__((swift_name("isNotCutEmpty()")));
- (BOOL)isNotEmpty __attribute__((swift_name("isNotEmpty()")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (SharedPosition * _Nullable)last __attribute__((swift_name("last()")));
- (id<SharedPolygonLoopIterator>)loopIterator __attribute__((swift_name("loopIterator()")));
- (SharedPosition * _Nullable)pop __attribute__((swift_name("pop()")));
- (BOOL)removeId:(int32_t)id __attribute__((swift_name("remove(id:)")));
- (id<SharedKotlinIterator>)reverseIterator __attribute__((swift_name("reverseIterator()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
@property (setter=setArea:) double area_ __attribute__((swift_name("area_")));
@property (readonly) int32_t cutSize __attribute__((swift_name("cutSize")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Polygon.Companion")))
@interface SharedPolygonCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPolygonCompanion *shared __attribute__((swift_name("shared")));
- (SharedPolygon *)fromPositionsPositions:(SharedKotlinArray<SharedPosition *> *)positions __attribute__((swift_name("fromPositions(positions:)")));
- (SharedPolygon *)fromPositionsPositions_:(NSArray<SharedPosition *> *)positions __attribute__((swift_name("fromPositions(positions_:)")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

__attribute__((swift_name("PolygonLoopIterator")))
@protocol SharedPolygonLoopIterator <SharedKotlinIterator>
@required
- (id _Nullable)viewCurrent __attribute__((swift_name("viewCurrent()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolygonUtils")))
@interface SharedPolygonUtils : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)polygonUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPolygonUtils *shared __attribute__((swift_name("shared")));
- (SharedPolygon *)toPolygon:(NSArray<SharedPosition *> *)receiver __attribute__((swift_name("toPolygon(_:)")));
- (void)clearSpatialIndexCache __attribute__((swift_name("clearSpatialIndexCache()")));
- (BOOL)containsPosition:(SharedPolygon *)receiver tap:(SharedPosition *)tap __attribute__((swift_name("containsPosition(_:tap:)")));
- (BOOL)containsPositionOptimized:(SharedPolygon *)receiver tap:(SharedPosition *)tap __attribute__((swift_name("containsPositionOptimized(_:tap:)")));
- (NSString *)convertPolygonsToGeoJsonPolygons:(NSArray<SharedPolygon *> *)polygons __attribute__((swift_name("convertPolygonsToGeoJson(polygons:)")));
- (BOOL)isPointInPolygonsTap:(SharedPosition *)tap polygons:(NSArray<SharedPolygon *> *)polygons __attribute__((swift_name("isPointInPolygons(tap:polygons:)")));
- (SharedBoundingBox *)polygonsBboxPolygons:(NSArray<SharedPolygon *> *)polygons __attribute__((swift_name("polygonsBbox(polygons:)")));
- (SharedPolygonUtilsSplitResult *)splitByLongitudePolygon:(SharedPolygon *)polygon lngToCut:(SharedComposedLongitude *)lngToCut __attribute__((swift_name("splitByLongitude(polygon:lngToCut:)")));
- (SharedPolygonUtilsSplitResult *)splitByLongitudePolygon:(SharedPolygon *)polygon lngToCut_:(double)lngToCut __attribute__((swift_name("splitByLongitude(polygon:lngToCut_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolygonUtilsQuad")))
@interface SharedPolygonUtilsQuad<A, B, C, D> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third fourth:(D _Nullable)fourth __attribute__((swift_name("init(first:second:third:fourth:)"))) __attribute__((objc_designated_initializer));
- (SharedPolygonUtilsQuad<A, B, C, D> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third fourth:(D _Nullable)fourth __attribute__((swift_name("doCopy(first:second:third:fourth:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) D _Nullable fourth __attribute__((swift_name("fourth")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@property (readonly) C _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolygonUtils.SplitResult")))
@interface SharedPolygonUtilsSplitResult : SharedBase
- (instancetype)initWithLeft:(NSArray<SharedPolygon *> *)left right:(NSArray<SharedPolygon *> *)right __attribute__((swift_name("init(left:right:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedPolygonUtilsSplitResultCompanion *companion __attribute__((swift_name("companion")));
- (SharedPolygonUtilsSplitResult *)doCopyLeft:(NSArray<SharedPolygon *> *)left right:(NSArray<SharedPolygon *> *)right __attribute__((swift_name("doCopy(left:right:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedPolygon *> *left __attribute__((swift_name("left")));
@property (readonly) NSArray<SharedPolygon *> *right __attribute__((swift_name("right")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PolygonUtils.SplitResultCompanion")))
@interface SharedPolygonUtilsSplitResultCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPolygonUtilsSplitResultCompanion *shared __attribute__((swift_name("shared")));
- (SharedPolygonUtilsSplitResult *)empty __attribute__((swift_name("empty()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Position.Companion")))
@interface SharedPositionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPositionCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Segment")))
@interface SharedSegment : SharedBase
- (instancetype)initWithStart:(SharedPosition *)start end:(SharedPosition *)end __attribute__((swift_name("init(start:end:)"))) __attribute__((objc_designated_initializer));
- (SharedSegment *)doCopyStart:(SharedPosition *)start end:(SharedPosition *)end __attribute__((swift_name("doCopy(start:end:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedPosition * _Nullable)intersectWithLngCutLng:(double)cutLng __attribute__((swift_name("intersectWithLng(cutLng:)")));
- (SharedCutPosition * _Nullable)intersectWithLngCutId:(int32_t)cutId cutLng:(double)cutLng __attribute__((swift_name("intersectWithLng(cutId:cutLng:)")));
- (SharedPosition * _Nullable)intersectWithSegmentOther:(SharedSegment *)other __attribute__((swift_name("intersectWithSegment(other:)")));
- (SharedCutPosition * _Nullable)intersectWithSegmentCutId:(int32_t)cutId other:(SharedSegment *)other __attribute__((swift_name("intersectWithSegment(cutId:other:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedPosition *end __attribute__((swift_name("end")));
@property (readonly) SharedPosition *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SystemClock")))
@interface SharedSystemClock : SharedBase <SharedIClock, SharedKoin_coreKoinComponent>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)delayDuration:(int64_t)duration completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("delay(duration:completionHandler:)")));
- (SharedKotlinInstant *)now __attribute__((swift_name("now()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DateTimeFormats")))
@interface SharedDateTimeFormats : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dateTimeFormats __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDateTimeFormats *shared __attribute__((swift_name("shared")));
- (NSString *)dayMonthInstant:(SharedKotlinInstant *)instant timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("dayMonth(instant:timeZone:)")));
- (NSString *)timeShortInstant:(SharedKotlinInstant *)instant timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("timeShort(instant:timeZone:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res")))
@interface SharedRes : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)res __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRes *shared __attribute__((swift_name("shared")));
- (NSString *)getUriPath:(NSString *)path __attribute__((swift_name("getUri(path:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)readBytesPath:(NSString *)path completionHandler:(void (^)(SharedKotlinByteArray * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("readBytes(path:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res.array")))
@interface SharedResArray : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)array __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedResArray *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res.drawable")))
@interface SharedResDrawable : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)drawable __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedResDrawable *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res.font")))
@interface SharedResFont : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)font __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedResFont *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res.plurals")))
@interface SharedResPlurals : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)plurals __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedResPlurals *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Res.string")))
@interface SharedResString : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)string __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedResString *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("AbstractEventMap")))
@interface SharedAbstractEventMap<T> : SharedBase <SharedKoin_coreKoinComponent>
- (instancetype)initWithEvent:(id<SharedIWWWEvent>)event mapConfig:(SharedEventMapConfig *)mapConfig onLocationUpdate:(void (^)(SharedPosition *))onLocationUpdate __attribute__((swift_name("init(event:mapConfig:onLocationUpdate:)"))) __attribute__((objc_designated_initializer));
- (SharedPosition * _Nullable)getCurrentPosition __attribute__((swift_name("getCurrentPosition()")));
- (SharedPositionManagerPositionSource * _Nullable)getCurrentPositionSource __attribute__((swift_name("getCurrentPositionSource()")));
- (void)markUserInteracted __attribute__((swift_name("markUserInteracted()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)moveToCenterOnComplete:(void (^)(void))onComplete completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("moveToCenter(onComplete:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)moveToMapBoundsOnComplete:(void (^)(void))onComplete completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("moveToMapBounds(onComplete:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)moveToWindowBoundsOnComplete:(void (^)(void))onComplete completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("moveToWindowBounds(onComplete:completionHandler:)")));
- (void)setupMapMap:(T _Nullable)map scope:(id<SharedKotlinx_coroutines_coreCoroutineScope>)scope stylePath:(NSString *)stylePath onMapLoaded:(void (^)(void))onMapLoaded onMapClick:(void (^ _Nullable)(SharedDouble *, SharedDouble *))onMapClick __attribute__((swift_name("setupMap(map:scope:stylePath:onMapLoaded:onMapClick:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)targetUserWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("targetUser(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)targetUserAndWaveWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("targetUserAndWave(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)targetWaveWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("targetWave(completionHandler:)")));
- (void)updateWavePolygonsWavePolygons:(NSArray<SharedPolygon *> *)wavePolygons clearPolygons:(BOOL)clearPolygons __attribute__((swift_name("updateWavePolygons(wavePolygons:clearPolygons:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SharedIWWWEvent> event __attribute__((swift_name("event")));
@property (readonly) id<SharedWWWLocationProvider> _Nullable locationProvider __attribute__((swift_name("locationProvider")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) SharedEventMapConfig *mapConfig __attribute__((swift_name("mapConfig")));
@property (readonly) id<SharedMapLibreAdapter> mapLibreAdapter __attribute__((swift_name("mapLibreAdapter")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CityMap")))
@interface SharedCityMap : SharedBase
- (instancetype)initWithId:(NSString *)id name:(NSString *)name isLoaded:(BOOL)isLoaded hasGeoJson:(BOOL)hasGeoJson loadTimestamp:(int64_t)loadTimestamp __attribute__((swift_name("init(id:name:isLoaded:hasGeoJson:loadTimestamp:)"))) __attribute__((objc_designated_initializer));
- (SharedCityMap *)doCopyId:(NSString *)id name:(NSString *)name isLoaded:(BOOL)isLoaded hasGeoJson:(BOOL)hasGeoJson loadTimestamp:(int64_t)loadTimestamp __attribute__((swift_name("doCopy(id:name:isLoaded:hasGeoJson:loadTimestamp:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL hasGeoJson __attribute__((swift_name("hasGeoJson")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) BOOL isLoaded __attribute__((swift_name("isLoaded")));
@property (readonly) int64_t loadTimestamp __attribute__((swift_name("loadTimestamp")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CityMapRegistry")))
@interface SharedCityMapRegistry : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cityMapRegistry __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCityMapRegistry *shared __attribute__((swift_name("shared")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)clearCacheWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("clearCache(completionHandler:)")));
- (id)getAllCityIds __attribute__((swift_name("getAllCityIds()")));
- (SharedCityMap * _Nullable)getLoadedMapCityId:(NSString *)cityId __attribute__((swift_name("getLoadedMap(cityId:)")));
- (SharedCityMapStatistics *)getStatistics __attribute__((swift_name("getStatistics()")));
- (BOOL)isAvailableCityId:(NSString *)cityId __attribute__((swift_name("isAvailable(cityId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loadMapIfNeededCityId:(NSString *)cityId completionHandler:(void (^)(SharedCityMap * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("loadMapIfNeeded(cityId:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CityMapStatistics")))
@interface SharedCityMapStatistics : SharedBase
- (instancetype)initWithTotalAvailableCities:(int32_t)totalAvailableCities loadedCities:(int32_t)loadedCities memoryFootprintMB:(double)memoryFootprintMB __attribute__((swift_name("init(totalAvailableCities:loadedCities:memoryFootprintMB:)"))) __attribute__((objc_designated_initializer));
- (SharedCityMapStatistics *)doCopyTotalAvailableCities:(int32_t)totalAvailableCities loadedCities:(int32_t)loadedCities memoryFootprintMB:(double)memoryFootprintMB __attribute__((swift_name("doCopy(totalAvailableCities:loadedCities:memoryFootprintMB:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t loadedCities __attribute__((swift_name("loadedCities")));
@property (readonly) double memoryFootprintMB __attribute__((swift_name("memoryFootprintMB")));
@property (readonly) int32_t totalAvailableCities __attribute__((swift_name("totalAvailableCities")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventMapConfig")))
@interface SharedEventMapConfig : SharedBase
- (instancetype)initWithInitialCameraPosition:(SharedMapCameraPosition *)initialCameraPosition autoTargetUserOnFirstLocation:(BOOL)autoTargetUserOnFirstLocation __attribute__((swift_name("init(initialCameraPosition:autoTargetUserOnFirstLocation:)"))) __attribute__((objc_designated_initializer));
- (SharedEventMapConfig *)doCopyInitialCameraPosition:(SharedMapCameraPosition *)initialCameraPosition autoTargetUserOnFirstLocation:(BOOL)autoTargetUserOnFirstLocation __attribute__((swift_name("doCopy(initialCameraPosition:autoTargetUserOnFirstLocation:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL autoTargetUserOnFirstLocation __attribute__((swift_name("autoTargetUserOnFirstLocation")));
@property (readonly) SharedMapCameraPosition *initialCameraPosition __attribute__((swift_name("initialCameraPosition")));
@end

__attribute__((swift_name("MapLibreAdapter")))
@protocol SharedMapLibreAdapter
@required
- (void)addOnCameraIdleListenerCallback:(void (^)(void))callback __attribute__((swift_name("addOnCameraIdleListener(callback:)")));
- (void)addWavePolygonsPolygons:(NSArray<id> *)polygons clearExisting:(BOOL)clearExisting __attribute__((swift_name("addWavePolygons(polygons:clearExisting:)")));
- (void)animateCameraPosition:(SharedPosition *)position zoom:(SharedDouble * _Nullable)zoom callback:(id<SharedMapCameraCallback> _Nullable)callback __attribute__((swift_name("animateCamera(position:zoom:callback:)")));
- (void)animateCameraToBoundsBounds:(SharedBoundingBox *)bounds padding:(int32_t)padding callback:(id<SharedMapCameraCallback> _Nullable)callback __attribute__((swift_name("animateCameraToBounds(bounds:padding:callback:)")));
- (void)drawOverridenBboxBbox:(SharedBoundingBox *)bbox __attribute__((swift_name("drawOverridenBbox(bbox:)")));
- (SharedPosition * _Nullable)getCameraPosition __attribute__((swift_name("getCameraPosition()")));
- (double)getHeight __attribute__((swift_name("getHeight()")));
- (double)getMinZoomLevel __attribute__((swift_name("getMinZoomLevel()")));
- (SharedBoundingBox *)getVisibleRegion __attribute__((swift_name("getVisibleRegion()")));
- (double)getWidth __attribute__((swift_name("getWidth()")));
- (void)moveCameraBounds:(SharedBoundingBox *)bounds __attribute__((swift_name("moveCamera(bounds:)")));
- (void)onMapSetCallback:(void (^)(id<SharedMapLibreAdapter>))callback __attribute__((swift_name("onMapSet(callback:)")));
- (void)setAttributionMarginsLeft:(int32_t)left top:(int32_t)top right:(int32_t)right bottom:(int32_t)bottom __attribute__((swift_name("setAttributionMargins(left:top:right:bottom:)")));
- (void)setBoundsForCameraTargetConstraintBounds:(SharedBoundingBox *)constraintBounds __attribute__((swift_name("setBoundsForCameraTarget(constraintBounds:)")));
- (void)setMapMap:(id _Nullable)map __attribute__((swift_name("setMap(map:)")));
- (void)setMaxZoomPreferenceMaxZoom:(double)maxZoom __attribute__((swift_name("setMaxZoomPreference(maxZoom:)")));
- (void)setMinZoomPreferenceMinZoom:(double)minZoom __attribute__((swift_name("setMinZoomPreference(minZoom:)")));
- (void)setOnMapClickListenerListener:(void (^ _Nullable)(SharedDouble *, SharedDouble *))listener __attribute__((swift_name("setOnMapClickListener(listener:)")));
- (void)setStyleStylePath:(NSString *)stylePath callback:(SharedKotlinUnit * _Nullable (^)(void))callback __attribute__((swift_name("setStyle(stylePath:callback:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentPosition __attribute__((swift_name("currentPosition")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentZoom __attribute__((swift_name("currentZoom")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSMapLibreAdapter")))
@interface SharedIOSMapLibreAdapter : SharedBase <SharedMapLibreAdapter>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addOnCameraIdleListenerCallback:(void (^)(void))callback __attribute__((swift_name("addOnCameraIdleListener(callback:)")));
- (void)addWavePolygonsPolygons:(NSArray<id> *)polygons clearExisting:(BOOL)clearExisting __attribute__((swift_name("addWavePolygons(polygons:clearExisting:)")));
- (void)animateCameraPosition:(SharedPosition *)position zoom:(SharedDouble * _Nullable)zoom callback:(id<SharedMapCameraCallback> _Nullable)callback __attribute__((swift_name("animateCamera(position:zoom:callback:)")));
- (void)animateCameraToBoundsBounds:(SharedBoundingBox *)bounds padding:(int32_t)padding callback:(id<SharedMapCameraCallback> _Nullable)callback __attribute__((swift_name("animateCameraToBounds(bounds:padding:callback:)")));
- (void)drawOverridenBboxBbox:(SharedBoundingBox *)bbox __attribute__((swift_name("drawOverridenBbox(bbox:)")));
- (SharedPosition * _Nullable)getCameraPosition __attribute__((swift_name("getCameraPosition()")));
- (double)getHeight __attribute__((swift_name("getHeight()")));
- (double)getMinZoomLevel __attribute__((swift_name("getMinZoomLevel()")));
- (SharedBoundingBox *)getVisibleRegion __attribute__((swift_name("getVisibleRegion()")));
- (double)getWidth __attribute__((swift_name("getWidth()")));
- (void)moveCameraBounds:(SharedBoundingBox *)bounds __attribute__((swift_name("moveCamera(bounds:)")));
- (void)onMapSetCallback:(void (^)(id<SharedMapLibreAdapter>))callback __attribute__((swift_name("onMapSet(callback:)")));
- (void)setAttributionMarginsLeft:(int32_t)left top:(int32_t)top right:(int32_t)right bottom:(int32_t)bottom __attribute__((swift_name("setAttributionMargins(left:top:right:bottom:)")));
- (void)setBoundsForCameraTargetConstraintBounds:(SharedBoundingBox *)constraintBounds __attribute__((swift_name("setBoundsForCameraTarget(constraintBounds:)")));
- (void)setMapMap:(id)map __attribute__((swift_name("setMap(map:)")));
- (void)setMaxZoomPreferenceMaxZoom:(double)maxZoom __attribute__((swift_name("setMaxZoomPreference(maxZoom:)")));
- (void)setMinZoomPreferenceMinZoom:(double)minZoom __attribute__((swift_name("setMinZoomPreference(minZoom:)")));
- (void)setOnMapClickListenerListener:(void (^ _Nullable)(SharedDouble *, SharedDouble *))listener __attribute__((swift_name("setOnMapClickListener(listener:)")));
- (void)setStyleStylePath:(NSString *)stylePath callback:(SharedKotlinUnit * _Nullable (^)(void))callback __attribute__((swift_name("setStyle(stylePath:callback:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentPosition __attribute__((swift_name("currentPosition")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentZoom __attribute__((swift_name("currentZoom")));
@end

__attribute__((swift_name("PlatformMapManager")))
@protocol SharedPlatformMapManager
@required
- (void)cancelDownloadMapId:(NSString *)mapId __attribute__((swift_name("cancelDownload(mapId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)downloadMapMapId:(NSString *)mapId onProgress:(void (^)(SharedInt *))onProgress onSuccess:(void (^)(void))onSuccess onError:(void (^)(SharedInt *, NSString * _Nullable))onError completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("downloadMap(mapId:onProgress:onSuccess:onError:completionHandler:)")));
- (BOOL)isMapAvailableMapId:(NSString *)mapId __attribute__((swift_name("isMapAvailable(mapId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSPlatformMapManager")))
@interface SharedIOSPlatformMapManager : SharedBase <SharedPlatformMapManager>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SharedIOSPlatformMapManagerCompanion *companion __attribute__((swift_name("companion")));
- (void)cancelDownloadMapId:(NSString *)mapId __attribute__((swift_name("cancelDownload(mapId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)downloadMapMapId:(NSString *)mapId onProgress:(void (^)(SharedInt *))onProgress onSuccess:(void (^)(void))onSuccess onError:(void (^)(SharedInt *, NSString * _Nullable))onError completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("downloadMap(mapId:onProgress:onSuccess:onError:completionHandler:)")));
- (BOOL)isMapAvailableMapId:(NSString *)mapId __attribute__((swift_name("isMapAvailable(mapId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSPlatformMapManager.Companion")))
@interface SharedIOSPlatformMapManagerCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedIOSPlatformMapManagerCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("WWWLocationProvider")))
@protocol SharedWWWLocationProvider
@required
- (void)startLocationUpdatesOnLocationUpdate:(void (^)(SharedPosition *))onLocationUpdate __attribute__((swift_name("startLocationUpdates(onLocationUpdate:)")));
- (void)stopLocationUpdates __attribute__((swift_name("stopLocationUpdates()")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentLocation __attribute__((swift_name("currentLocation")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSWWWLocationProvider")))
@interface SharedIOSWWWLocationProvider : SharedBase <SharedWWWLocationProvider>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)startLocationUpdatesOnLocationUpdate:(void (^)(SharedPosition *))onLocationUpdate __attribute__((swift_name("startLocationUpdates(onLocationUpdate:)")));
- (void)stopLocationUpdates __attribute__((swift_name("stopLocationUpdates()")));
- (void)updateLocationFromNativeLatitude:(double)latitude longitude:(double)longitude __attribute__((swift_name("updateLocationFromNative(latitude:longitude:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentLocation __attribute__((swift_name("currentLocation")));
@end

__attribute__((swift_name("MapCameraCallback")))
@protocol SharedMapCameraCallback
@required
- (void)onCancel __attribute__((swift_name("onCancel()")));
- (void)onFinish __attribute__((swift_name("onFinish()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapCameraPosition")))
@interface SharedMapCameraPosition : SharedKotlinEnum<SharedMapCameraPosition *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedMapCameraPosition *window __attribute__((swift_name("window")));
@property (class, readonly) SharedMapCameraPosition *bounds __attribute__((swift_name("bounds")));
@property (class, readonly) SharedMapCameraPosition *defaultCenter __attribute__((swift_name("defaultCenter")));
+ (SharedKotlinArray<SharedMapCameraPosition *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMapCameraPosition *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapConstraintManager")))
@interface SharedMapConstraintManager : SharedBase
- (instancetype)initWithMapBounds:(SharedBoundingBox *)mapBounds mapLibreAdapter:(id<SharedMapLibreAdapter>)mapLibreAdapter isSuppressed:(SharedBoolean *(^)(void))isSuppressed __attribute__((swift_name("init(mapBounds:mapLibreAdapter:isSuppressed:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   androidx.annotation.UiThread
*/
- (void)applyConstraints __attribute__((swift_name("applyConstraints()")));
- (SharedBoundingBox *)calculateConstraintBounds __attribute__((swift_name("calculateConstraintBounds()")));
- (SharedBoundingBox *)calculateSafeBoundsCenterPosition:(SharedPosition *)centerPosition __attribute__((swift_name("calculateSafeBounds(centerPosition:)")));
- (void)constrainCamera __attribute__((swift_name("constrainCamera()")));
- (SharedPosition *)getNearestValidPointPoint:(SharedPosition *)point bounds:(SharedBoundingBox *)bounds __attribute__((swift_name("getNearestValidPoint(point:bounds:)")));
- (BOOL)hasSignificantPaddingChangeNewPadding:(SharedMapConstraintManagerVisibleRegionPadding *)newPadding __attribute__((swift_name("hasSignificantPaddingChange(newPadding:)")));
- (BOOL)isValidBoundsBounds:(SharedBoundingBox *)bounds currentPosition:(SharedPosition * _Nullable)currentPosition __attribute__((swift_name("isValidBounds(bounds:currentPosition:)")));
- (void)setVisibleRegionPaddingPadding:(SharedMapConstraintManagerVisibleRegionPadding *)padding __attribute__((swift_name("setVisibleRegionPadding(padding:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapConstraintManager.VisibleRegionPadding")))
@interface SharedMapConstraintManagerVisibleRegionPadding : SharedBase
- (instancetype)initWithLatPadding:(double)latPadding lngPadding:(double)lngPadding __attribute__((swift_name("init(latPadding:lngPadding:)"))) __attribute__((objc_designated_initializer));
- (SharedMapConstraintManagerVisibleRegionPadding *)doCopyLatPadding:(double)latPadding lngPadding:(double)lngPadding __attribute__((swift_name("doCopy(latPadding:lngPadding:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property double latPadding __attribute__((swift_name("latPadding")));
@property double lngPadding __attribute__((swift_name("lngPadding")));
@end

__attribute__((swift_name("MapFeatureState")))
@interface SharedMapFeatureState : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Available")))
@interface SharedMapFeatureStateAvailable : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)available __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateAvailable *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Canceling")))
@interface SharedMapFeatureStateCanceling : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)canceling __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateCanceling *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Downloading")))
@interface SharedMapFeatureStateDownloading : SharedMapFeatureState
- (instancetype)initWithProgress:(int32_t)progress __attribute__((swift_name("init(progress:)"))) __attribute__((objc_designated_initializer));
- (SharedMapFeatureStateDownloading *)doCopyProgress:(int32_t)progress __attribute__((swift_name("doCopy(progress:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t progress __attribute__((swift_name("progress")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Failed")))
@interface SharedMapFeatureStateFailed : SharedMapFeatureState
- (instancetype)initWithErrorCode:(int32_t)errorCode errorMessage:(NSString * _Nullable)errorMessage __attribute__((swift_name("init(errorCode:errorMessage:)"))) __attribute__((objc_designated_initializer));
- (SharedMapFeatureStateFailed *)doCopyErrorCode:(int32_t)errorCode errorMessage:(NSString * _Nullable)errorMessage __attribute__((swift_name("doCopy(errorCode:errorMessage:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t errorCode __attribute__((swift_name("errorCode")));
@property (readonly) NSString * _Nullable errorMessage __attribute__((swift_name("errorMessage")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Installed")))
@interface SharedMapFeatureStateInstalled : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)installed __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateInstalled *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Installing")))
@interface SharedMapFeatureStateInstalling : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)installing __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateInstalling *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.NotAvailable")))
@interface SharedMapFeatureStateNotAvailable : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)notAvailable __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateNotAvailable *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.NotChecked")))
@interface SharedMapFeatureStateNotChecked : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)notChecked __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateNotChecked *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Pending")))
@interface SharedMapFeatureStatePending : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)pending __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStatePending *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Retrying")))
@interface SharedMapFeatureStateRetrying : SharedMapFeatureState
- (instancetype)initWithAttempt:(int32_t)attempt maxAttempts:(int32_t)maxAttempts __attribute__((swift_name("init(attempt:maxAttempts:)"))) __attribute__((objc_designated_initializer));
- (SharedMapFeatureStateRetrying *)doCopyAttempt:(int32_t)attempt maxAttempts:(int32_t)maxAttempts __attribute__((swift_name("doCopy(attempt:maxAttempts:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t attempt __attribute__((swift_name("attempt")));
@property (readonly) int32_t maxAttempts __attribute__((swift_name("maxAttempts")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFeatureState.Unknown")))
@interface SharedMapFeatureStateUnknown : SharedMapFeatureState
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unknown __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFeatureStateUnknown *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapStateManager")))
@interface SharedMapStateManager : SharedBase
- (instancetype)initWithPlatformMapManager:(id<SharedPlatformMapManager>)platformMapManager __attribute__((swift_name("init(platformMapManager:)"))) __attribute__((objc_designated_initializer));
- (void)cancelDownload __attribute__((swift_name("cancelDownload()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)checkMapAvailabilityMapId:(NSString *)mapId autoDownload:(BOOL)autoDownload completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("checkMapAvailability(mapId:autoDownload:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)downloadMapMapId:(NSString *)mapId completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("downloadMap(mapId:completionHandler:)")));
- (void)refreshAvailability __attribute__((swift_name("refreshAvailability()")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> featureState __attribute__((swift_name("featureState")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> mapStates __attribute__((swift_name("mapStates")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PositionManager")))
@interface SharedPositionManager : SharedBase
- (instancetype)initWithCoroutineScopeProvider:(id<SharedCoroutineScopeProvider>)coroutineScopeProvider debounceDelay:(int64_t)debounceDelay positionEpsilon:(double)positionEpsilon __attribute__((swift_name("init(coroutineScopeProvider:debounceDelay:positionEpsilon:)"))) __attribute__((objc_designated_initializer));
- (void)cleanup __attribute__((swift_name("cleanup()")));
- (void)clearAll __attribute__((swift_name("clearAll()")));
- (void)clearPositionSource:(SharedPositionManagerPositionSource *)source __attribute__((swift_name("clearPosition(source:)")));
- (SharedPosition * _Nullable)getCurrentPosition __attribute__((swift_name("getCurrentPosition()")));
- (SharedPositionManagerPositionSource * _Nullable)getCurrentSource __attribute__((swift_name("getCurrentSource()")));
- (void)updatePositionSource:(SharedPositionManagerPositionSource *)source newPosition:(SharedPosition * _Nullable)newPosition __attribute__((swift_name("updatePosition(source:newPosition:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> position __attribute__((swift_name("position")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PositionManager.PositionSource")))
@interface SharedPositionManagerPositionSource : SharedKotlinEnum<SharedPositionManagerPositionSource *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedPositionManagerPositionSource *simulation __attribute__((swift_name("simulation")));
@property (class, readonly) SharedPositionManagerPositionSource *gps __attribute__((swift_name("gps")));
+ (SharedKotlinArray<SharedPositionManagerPositionSource *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPositionManagerPositionSource *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("AudioBuffer")))
@protocol SharedAudioBuffer
@required
- (SharedKotlinByteArray *)getRawBuffer __attribute__((swift_name("getRawBuffer()")));
@property (readonly) int32_t sampleCount __attribute__((swift_name("sampleCount")));
@property (readonly) int32_t sampleRate __attribute__((swift_name("sampleRate")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AudioBufferFactory")))
@interface SharedAudioBufferFactory : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)audioBufferFactory __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAudioBufferFactory *shared __attribute__((swift_name("shared")));
- (id<SharedAudioBuffer>)createFromSamplesSamples:(SharedKotlinDoubleArray *)samples sampleRate:(int32_t)sampleRate bitsPerSample:(int32_t)bitsPerSample channels:(int32_t)channels __attribute__((swift_name("createFromSamples(samples:sampleRate:bitsPerSample:channels:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSAudioBuffer")))
@interface SharedIOSAudioBuffer : SharedBase <SharedAudioBuffer>
- (instancetype)initWithSamples:(SharedKotlinDoubleArray *)samples sampleRate:(int32_t)sampleRate bitsPerSample:(int32_t)bitsPerSample channels:(int32_t)channels __attribute__((swift_name("init(samples:sampleRate:bitsPerSample:channels:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinByteArray *)getRawBuffer __attribute__((swift_name("getRawBuffer()")));
@property (readonly) int32_t sampleCount __attribute__((swift_name("sampleCount")));
@property (readonly) int32_t sampleRate __attribute__((swift_name("sampleRate")));
@end

__attribute__((swift_name("SoundPlayer")))
@protocol SharedSoundPlayer
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)playToneFrequency:(double)frequency amplitude:(double)amplitude duration:(int64_t)duration waveform:(SharedSoundPlayerWaveform *)waveform completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("playTone(frequency:amplitude:duration:waveform:completionHandler:)")));
- (void)release_ __attribute__((swift_name("release()")));
@end

__attribute__((swift_name("VolumeController")))
@protocol SharedVolumeController
@required
- (float)getCurrentVolume __attribute__((swift_name("getCurrentVolume()")));
- (void)setVolumeLevel:(float)level __attribute__((swift_name("setVolume(level:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSSoundPlayer")))
@interface SharedIOSSoundPlayer : SharedBase <SharedSoundPlayer, SharedVolumeController>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (float)getCurrentVolume __attribute__((swift_name("getCurrentVolume()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)playToneFrequency:(double)frequency amplitude:(double)amplitude duration:(int64_t)duration waveform:(SharedSoundPlayerWaveform *)waveform completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("playTone(frequency:amplitude:duration:waveform:completionHandler:)")));
- (void)release_ __attribute__((swift_name("release()")));
- (void)setVolumeLevel:(float)level __attribute__((swift_name("setVolume(level:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MidiNote")))
@interface SharedMidiNote : SharedBase
- (instancetype)initWithPitch:(int32_t)pitch velocity:(int32_t)velocity startTime:(int64_t)startTime duration:(int64_t)duration __attribute__((swift_name("init(pitch:velocity:startTime:duration:)"))) __attribute__((objc_designated_initializer));
- (SharedMidiNote *)doCopyPitch:(int32_t)pitch velocity:(int32_t)velocity startTime:(int64_t)startTime duration:(int64_t)duration __attribute__((swift_name("doCopy(pitch:velocity:startTime:duration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isActiveAtTimePosition:(int64_t)timePosition __attribute__((swift_name("isActiveAt(timePosition:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t duration __attribute__((swift_name("duration")));
@property (readonly) int32_t pitch __attribute__((swift_name("pitch")));
@property (readonly) int64_t startTime __attribute__((swift_name("startTime")));
@property (readonly) int32_t velocity __attribute__((swift_name("velocity")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MidiParser")))
@interface SharedMidiParser : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)midiParser __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMidiParser *shared __attribute__((swift_name("shared")));
- (SharedMidiTrack *)parseMidiBytesBytes:(SharedKotlinByteArray *)bytes __attribute__((swift_name("parseMidiBytes(bytes:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)parseMidiFileMidiResourcePath:(NSString *)midiResourcePath completionHandler:(void (^)(SharedMidiTrack * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("parseMidiFile(midiResourcePath:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MidiResources")))
@interface SharedMidiResources : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)midiResources __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMidiResources *shared __attribute__((swift_name("shared")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)readMidiFilePath:(NSString *)path completionHandler:(void (^)(SharedKotlinByteArray * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("readMidiFile(path:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MidiTrack")))
@interface SharedMidiTrack : SharedBase
- (instancetype)initWithName:(NSString *)name notes:(NSArray<SharedMidiNote *> *)notes totalDuration:(int64_t)totalDuration tempo:(int32_t)tempo __attribute__((swift_name("init(name:notes:totalDuration:tempo:)"))) __attribute__((objc_designated_initializer));
- (SharedMidiTrack *)doCopyName:(NSString *)name notes:(NSArray<SharedMidiNote *> *)notes totalDuration:(int64_t)totalDuration tempo:(int32_t)tempo __attribute__((swift_name("doCopy(name:notes:totalDuration:tempo:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSArray<SharedMidiNote *> *notes __attribute__((swift_name("notes")));
@property (readonly) int32_t tempo __attribute__((swift_name("tempo")));
@property (readonly) int64_t totalDuration __attribute__((swift_name("totalDuration")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SoundPlayerWaveform")))
@interface SharedSoundPlayerWaveform : SharedKotlinEnum<SharedSoundPlayerWaveform *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSoundPlayerWaveform *sine __attribute__((swift_name("sine")));
@property (class, readonly) SharedSoundPlayerWaveform *square __attribute__((swift_name("square")));
@property (class, readonly) SharedSoundPlayerWaveform *triangle __attribute__((swift_name("triangle")));
@property (class, readonly) SharedSoundPlayerWaveform *sawtooth __attribute__((swift_name("sawtooth")));
+ (SharedKotlinArray<SharedSoundPlayerWaveform *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSoundPlayerWaveform *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WaveformGenerator")))
@interface SharedWaveformGenerator : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)waveformGenerator __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWaveformGenerator *shared __attribute__((swift_name("shared")));
- (SharedKotlinDoubleArray *)generateWaveformSampleRate:(int32_t)sampleRate frequency:(double)frequency amplitude:(double)amplitude duration:(int64_t)duration waveform:(SharedSoundPlayerWaveform *)waveform __attribute__((swift_name("generateWaveform(sampleRate:frequency:amplitude:duration:waveform:)")));
- (double)midiPitchToFrequencyPitch:(int32_t)pitch __attribute__((swift_name("midiPitchToFrequency(pitch:)")));
- (double)midiVelocityToAmplitudeVelocity:(int32_t)velocity __attribute__((swift_name("midiVelocityToAmplitude(velocity:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BackgroundTaskUsage")))
@interface SharedBackgroundTaskUsage : SharedBase
- (instancetype)initWithNonEssentialTasksLimited:(BOOL)nonEssentialTasksLimited essentialTasksMaintained:(BOOL)essentialTasksMaintained __attribute__((swift_name("init(nonEssentialTasksLimited:essentialTasksMaintained:)"))) __attribute__((objc_designated_initializer));
- (SharedBackgroundTaskUsage *)doCopyNonEssentialTasksLimited:(BOOL)nonEssentialTasksLimited essentialTasksMaintained:(BOOL)essentialTasksMaintained __attribute__((swift_name("doCopy(nonEssentialTasksLimited:essentialTasksMaintained:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL essentialTasksMaintained __attribute__((swift_name("essentialTasksMaintained")));
@property (readonly) BOOL nonEssentialTasksLimited __attribute__((swift_name("nonEssentialTasksLimited")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BatteryUsage")))
@interface SharedBatteryUsage : SharedBase
- (instancetype)initWithTotalPowerMah:(double)totalPowerMah backgroundCpuMs:(int64_t)backgroundCpuMs averageCpuPercent:(double)averageCpuPercent __attribute__((swift_name("init(totalPowerMah:backgroundCpuMs:averageCpuPercent:)"))) __attribute__((objc_designated_initializer));
- (SharedBatteryUsage *)doCopyTotalPowerMah:(double)totalPowerMah backgroundCpuMs:(int64_t)backgroundCpuMs averageCpuPercent:(double)averageCpuPercent __attribute__((swift_name("doCopy(totalPowerMah:backgroundCpuMs:averageCpuPercent:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double averageCpuPercent __attribute__((swift_name("averageCpuPercent")));
@property (readonly) int64_t backgroundCpuMs __attribute__((swift_name("backgroundCpuMs")));
@property (readonly) double totalPowerMah __attribute__((swift_name("totalPowerMah")));
@end

__attribute__((swift_name("IPerformanceMonitor")))
@protocol SharedIPerformanceMonitor
@required
- (SharedPerformanceReport *)getPerformanceReport __attribute__((swift_name("getPerformanceReport()")));
- (void)recordAnimationPerformanceAnimationName:(NSString *)animationName frameDrops:(int32_t)frameDrops __attribute__((swift_name("recordAnimationPerformance(animationName:frameDrops:)")));
- (void)recordChoreographyPerformanceSequenceId:(NSString *)sequenceId renderTime:(int64_t)renderTime __attribute__((swift_name("recordChoreographyPerformance(sequenceId:renderTime:)")));
- (void)recordEventName:(NSString *)name parameters:(NSDictionary<NSString *, id> *)parameters __attribute__((swift_name("recordEvent(name:parameters:)")));
- (void)recordLocationAccuracyAccuracy:(float)accuracy __attribute__((swift_name("recordLocationAccuracy(accuracy:)")));
- (void)recordMemoryUsageUsed:(int64_t)used available:(int64_t)available __attribute__((swift_name("recordMemoryUsage(used:available:)")));
- (void)recordMetricName:(NSString *)name value:(double)value unit:(NSString *)unit __attribute__((swift_name("recordMetric(name:value:unit:)")));
- (void)recordNetworkLatencyEndpoint:(NSString *)endpoint latency:(int64_t)latency __attribute__((swift_name("recordNetworkLatency(endpoint:latency:)")));
- (void)recordScreenLoadScreenName:(NSString *)screenName loadTime:(int64_t)loadTime __attribute__((swift_name("recordScreenLoad(screenName:loadTime:)")));
- (void)recordUserInteractionAction:(NSString *)action responseTime:(int64_t)responseTime __attribute__((swift_name("recordUserInteraction(action:responseTime:)")));
- (void)recordWaveParticipationEventId:(NSString *)eventId participationSuccess:(BOOL)participationSuccess __attribute__((swift_name("recordWaveParticipation(eventId:participationSuccess:)")));
- (void)recordWaveTimingAccuracyExpectedTime:(int64_t)expectedTime actualTime:(int64_t)actualTime __attribute__((swift_name("recordWaveTimingAccuracy(expectedTime:actualTime:)")));
- (id<SharedPerformanceTrace>)startTraceName:(NSString *)name __attribute__((swift_name("startTrace(name:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> performanceMetrics __attribute__((swift_name("performanceMetrics")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PerformanceIssue")))
@interface SharedPerformanceIssue : SharedBase
- (instancetype)initWithSeverity:(SharedPerformanceIssueSeverity *)severity category:(SharedPerformanceIssueCategory *)category description:(NSString *)description impact:(NSString *)impact occurrence:(int64_t)occurrence __attribute__((swift_name("init(severity:category:description:impact:occurrence:)"))) __attribute__((objc_designated_initializer));
- (SharedPerformanceIssue *)doCopySeverity:(SharedPerformanceIssueSeverity *)severity category:(SharedPerformanceIssueCategory *)category description:(NSString *)description impact:(NSString *)impact occurrence:(int64_t)occurrence __attribute__((swift_name("doCopy(severity:category:description:impact:occurrence:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedPerformanceIssueCategory *category __attribute__((swift_name("category")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *impact __attribute__((swift_name("impact")));
@property (readonly) int64_t occurrence __attribute__((swift_name("occurrence")));
@property (readonly) SharedPerformanceIssueSeverity *severity __attribute__((swift_name("severity")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PerformanceIssue.Category")))
@interface SharedPerformanceIssueCategory : SharedKotlinEnum<SharedPerformanceIssueCategory *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedPerformanceIssueCategory *waveTiming __attribute__((swift_name("waveTiming")));
@property (class, readonly) SharedPerformanceIssueCategory *uiResponsiveness __attribute__((swift_name("uiResponsiveness")));
@property (class, readonly) SharedPerformanceIssueCategory *memory __attribute__((swift_name("memory")));
@property (class, readonly) SharedPerformanceIssueCategory *network __attribute__((swift_name("network")));
@property (class, readonly) SharedPerformanceIssueCategory *location __attribute__((swift_name("location")));
+ (SharedKotlinArray<SharedPerformanceIssueCategory *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPerformanceIssueCategory *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PerformanceIssue.Severity")))
@interface SharedPerformanceIssueSeverity : SharedKotlinEnum<SharedPerformanceIssueSeverity *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedPerformanceIssueSeverity *low __attribute__((swift_name("low")));
@property (class, readonly) SharedPerformanceIssueSeverity *medium __attribute__((swift_name("medium")));
@property (class, readonly) SharedPerformanceIssueSeverity *high __attribute__((swift_name("high")));
@property (class, readonly) SharedPerformanceIssueSeverity *critical __attribute__((swift_name("critical")));
+ (SharedKotlinArray<SharedPerformanceIssueSeverity *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPerformanceIssueSeverity *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PerformanceMetrics")))
@interface SharedPerformanceMetrics : SharedBase
- (instancetype)initWithAverageWaveTimingAccuracy:(double)averageWaveTimingAccuracy waveParticipationRate:(double)waveParticipationRate averageScreenLoadTime:(int64_t)averageScreenLoadTime averageNetworkLatency:(int64_t)averageNetworkLatency memoryUsagePercent:(double)memoryUsagePercent locationAccuracy:(float)locationAccuracy totalEvents:(int64_t)totalEvents lastUpdated:(int64_t)lastUpdated __attribute__((swift_name("init(averageWaveTimingAccuracy:waveParticipationRate:averageScreenLoadTime:averageNetworkLatency:memoryUsagePercent:locationAccuracy:totalEvents:lastUpdated:)"))) __attribute__((objc_designated_initializer));
- (SharedPerformanceMetrics *)doCopyAverageWaveTimingAccuracy:(double)averageWaveTimingAccuracy waveParticipationRate:(double)waveParticipationRate averageScreenLoadTime:(int64_t)averageScreenLoadTime averageNetworkLatency:(int64_t)averageNetworkLatency memoryUsagePercent:(double)memoryUsagePercent locationAccuracy:(float)locationAccuracy totalEvents:(int64_t)totalEvents lastUpdated:(int64_t)lastUpdated __attribute__((swift_name("doCopy(averageWaveTimingAccuracy:waveParticipationRate:averageScreenLoadTime:averageNetworkLatency:memoryUsagePercent:locationAccuracy:totalEvents:lastUpdated:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t averageNetworkLatency __attribute__((swift_name("averageNetworkLatency")));
@property (readonly) int64_t averageScreenLoadTime __attribute__((swift_name("averageScreenLoadTime")));
@property (readonly) double averageWaveTimingAccuracy __attribute__((swift_name("averageWaveTimingAccuracy")));
@property (readonly) int64_t lastUpdated __attribute__((swift_name("lastUpdated")));
@property (readonly) float locationAccuracy __attribute__((swift_name("locationAccuracy")));
@property (readonly) double memoryUsagePercent __attribute__((swift_name("memoryUsagePercent")));
@property (readonly) int64_t totalEvents __attribute__((swift_name("totalEvents")));
@property (readonly) double waveParticipationRate __attribute__((swift_name("waveParticipationRate")));
@end

__attribute__((swift_name("PerformanceMonitor")))
@interface SharedPerformanceMonitor : SharedBase <SharedIPerformanceMonitor>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedPerformanceReport *)getPerformanceReport __attribute__((swift_name("getPerformanceReport()")));
- (void)recordAnimationPerformanceAnimationName:(NSString *)animationName frameDrops:(int32_t)frameDrops __attribute__((swift_name("recordAnimationPerformance(animationName:frameDrops:)")));
- (void)recordChoreographyPerformanceSequenceId:(NSString *)sequenceId renderTime:(int64_t)renderTime __attribute__((swift_name("recordChoreographyPerformance(sequenceId:renderTime:)")));
- (void)recordEventName:(NSString *)name parameters:(NSDictionary<NSString *, id> *)parameters __attribute__((swift_name("recordEvent(name:parameters:)")));
- (void)recordLocationAccuracyAccuracy:(float)accuracy __attribute__((swift_name("recordLocationAccuracy(accuracy:)")));
- (void)recordMemoryUsageUsed:(int64_t)used available:(int64_t)available __attribute__((swift_name("recordMemoryUsage(used:available:)")));
- (void)recordMetricName:(NSString *)name value:(double)value unit:(NSString *)unit __attribute__((swift_name("recordMetric(name:value:unit:)")));
- (void)recordNetworkLatencyEndpoint:(NSString *)endpoint latency:(int64_t)latency __attribute__((swift_name("recordNetworkLatency(endpoint:latency:)")));
- (void)recordScreenLoadScreenName:(NSString *)screenName loadTime:(int64_t)loadTime __attribute__((swift_name("recordScreenLoad(screenName:loadTime:)")));
- (void)recordUserInteractionAction:(NSString *)action responseTime:(int64_t)responseTime __attribute__((swift_name("recordUserInteraction(action:responseTime:)")));
- (void)recordWaveParticipationEventId:(NSString *)eventId participationSuccess:(BOOL)participationSuccess __attribute__((swift_name("recordWaveParticipation(eventId:participationSuccess:)")));
- (void)recordWaveTimingAccuracyExpectedTime:(int64_t)expectedTime actualTime:(int64_t)actualTime __attribute__((swift_name("recordWaveTimingAccuracy(expectedTime:actualTime:)")));
- (id<SharedPerformanceTrace>)startTraceName:(NSString *)name __attribute__((swift_name("startTrace(name:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> performanceMetrics __attribute__((swift_name("performanceMetrics")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PerformanceReport")))
@interface SharedPerformanceReport : SharedBase
- (instancetype)initWithAppVersion:(NSString *)appVersion platform:(NSString *)platform deviceInfo:(NSString *)deviceInfo reportPeriod:(int64_t)reportPeriod metrics:(SharedPerformanceMetrics *)metrics criticalIssues:(NSArray<SharedPerformanceIssue *> *)criticalIssues recommendations:(NSArray<NSString *> *)recommendations __attribute__((swift_name("init(appVersion:platform:deviceInfo:reportPeriod:metrics:criticalIssues:recommendations:)"))) __attribute__((objc_designated_initializer));
- (SharedPerformanceReport *)doCopyAppVersion:(NSString *)appVersion platform:(NSString *)platform deviceInfo:(NSString *)deviceInfo reportPeriod:(int64_t)reportPeriod metrics:(SharedPerformanceMetrics *)metrics criticalIssues:(NSArray<SharedPerformanceIssue *> *)criticalIssues recommendations:(NSArray<NSString *> *)recommendations __attribute__((swift_name("doCopy(appVersion:platform:deviceInfo:reportPeriod:metrics:criticalIssues:recommendations:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *appVersion __attribute__((swift_name("appVersion")));
@property (readonly) NSArray<SharedPerformanceIssue *> *criticalIssues __attribute__((swift_name("criticalIssues")));
@property (readonly) NSString *deviceInfo __attribute__((swift_name("deviceInfo")));
@property (readonly) SharedPerformanceMetrics *metrics __attribute__((swift_name("metrics")));
@property (readonly) NSString *platform __attribute__((swift_name("platform")));
@property (readonly) NSArray<NSString *> *recommendations __attribute__((swift_name("recommendations")));
@property (readonly) int64_t reportPeriod __attribute__((swift_name("reportPeriod")));
@end

__attribute__((swift_name("PerformanceTrace")))
@protocol SharedPerformanceTrace
@required
- (void)addAttributeKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("addAttribute(key:value:)")));
- (void)addMetricKey:(NSString *)key value:(int64_t)value __attribute__((swift_name("addMetric(key:value:)")));
- (SharedBackgroundTaskUsage *)getBackgroundTaskUsage __attribute__((swift_name("getBackgroundTaskUsage()")));
- (SharedBatteryUsage *)getBatteryUsage __attribute__((swift_name("getBatteryUsage()")));
- (int64_t)getDurationMs __attribute__((swift_name("getDurationMs()")));
- (void)stop __attribute__((swift_name("stop()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int64_t startTime __attribute__((swift_name("startTime")));
@end

__attribute__((swift_name("AppScreen")))
@interface SharedAppScreen : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppScreen.About")))
@interface SharedAppScreenAbout : SharedAppScreen
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)about __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppScreenAbout *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppScreen.EventDetails")))
@interface SharedAppScreenEventDetails : SharedAppScreen
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eventDetails __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppScreenEventDetails *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppScreen.EventsList")))
@interface SharedAppScreenEventsList : SharedAppScreen
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eventsList __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppScreenEventsList *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppScreen.Map")))
@interface SharedAppScreenMap : SharedAppScreen
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)map __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppScreenMap *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppScreen.Wave")))
@interface SharedAppScreenWave : SharedAppScreen
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)wave __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppScreenWave *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("BaseViewModel")))
@interface SharedBaseViewModel : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)clear __attribute__((swift_name("clear()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)onCleared __attribute__((swift_name("onCleared()")));
@property (readonly) id<SharedKotlinx_coroutines_coreCoroutineScope> viewModelScope __attribute__((swift_name("viewModelScope")));
@end

__attribute__((swift_name("IOSLifecycleObserver")))
@protocol SharedIOSLifecycleObserver
@required
- (void)onViewDeinit __attribute__((swift_name("onViewDeinit()")));
- (void)onViewDidAppear __attribute__((swift_name("onViewDidAppear()")));
- (void)onViewDidDisappear __attribute__((swift_name("onViewDidDisappear()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSLifecycleObserverImpl")))
@interface SharedIOSLifecycleObserverImpl : SharedBase <SharedIOSLifecycleObserver>
- (instancetype)initWithSubscriptionManager:(id<SharedIOSReactiveSubscriptionManager>)subscriptionManager __attribute__((swift_name("init(subscriptionManager:)"))) __attribute__((objc_designated_initializer));
- (void)onViewDeinit __attribute__((swift_name("onViewDeinit()")));
- (void)onViewDidAppear __attribute__((swift_name("onViewDidAppear()")));
- (void)onViewDidDisappear __attribute__((swift_name("onViewDidDisappear()")));
@end

__attribute__((swift_name("IOSObservable")))
@protocol SharedIOSObservable
@required
- (id<SharedIOSObservableSubscription>)observeCallback:(void (^)(id _Nullable))callback __attribute__((swift_name("observe(callback:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)observeAsyncCallback:(id<SharedKotlinSuspendFunction1>)callback completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("observeAsync(callback:completionHandler:)")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((swift_name("IOSObservableSubscription")))
@protocol SharedIOSObservableSubscription
@required
- (void)dispose __attribute__((swift_name("dispose()")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@end

__attribute__((swift_name("IOSReactiveSubscriptionManager")))
@protocol SharedIOSReactiveSubscriptionManager
@required
- (void)addSubscriptionSubscription:(id<SharedIOSObservableSubscription>)subscription __attribute__((swift_name("addSubscription(subscription:)")));
- (void)disposeAll __attribute__((swift_name("disposeAll()")));
@property (readonly) int32_t activeSubscriptionCount __attribute__((swift_name("activeSubscriptionCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSReactiveSubscriptionManagerImpl")))
@interface SharedIOSReactiveSubscriptionManagerImpl : SharedBase <SharedIOSReactiveSubscriptionManager>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)addSubscriptionSubscription:(id<SharedIOSObservableSubscription>)subscription __attribute__((swift_name("addSubscription(subscription:)")));
- (void)disposeAll __attribute__((swift_name("disposeAll()")));
@property (readonly) int32_t activeSubscriptionCount __attribute__((swift_name("activeSubscriptionCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TabManager")))
@interface SharedTabManager : SharedBase
@end

__attribute__((swift_name("TabScreen")))
@protocol SharedTabScreen
@required
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((swift_name("WaveNavigator")))
@protocol SharedWaveNavigator
@required
- (void)navigateToWaveEventId:(NSString *)eventId __attribute__((swift_name("navigateToWave(eventId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WaveState")))
@interface SharedWaveState : SharedBase
- (instancetype)initWithIsWarmingInProgress:(BOOL)isWarmingInProgress hitDateTime:(SharedKotlinInstant *)hitDateTime isGoingToBeHit:(BOOL)isGoingToBeHit hasBeenHit:(BOOL)hasBeenHit __attribute__((swift_name("init(isWarmingInProgress:hitDateTime:isGoingToBeHit:hasBeenHit:)"))) __attribute__((objc_designated_initializer));
- (SharedWaveState *)doCopyIsWarmingInProgress:(BOOL)isWarmingInProgress hitDateTime:(SharedKotlinInstant *)hitDateTime isGoingToBeHit:(BOOL)isGoingToBeHit hasBeenHit:(BOOL)hasBeenHit __attribute__((swift_name("doCopy(isWarmingInProgress:hitDateTime:isGoingToBeHit:hasBeenHit:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL hasBeenHit __attribute__((swift_name("hasBeenHit")));
@property (readonly) SharedKotlinInstant *hitDateTime __attribute__((swift_name("hitDateTime")));
@property (readonly) BOOL isGoingToBeHit __attribute__((swift_name("isGoingToBeHit")));
@property (readonly) BOOL isWarmingInProgress __attribute__((swift_name("isWarmingInProgress")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TabConfiguration")))
@interface SharedTabConfiguration : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)tabConfiguration __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTabConfiguration *shared __attribute__((swift_name("shared")));
- (NSArray<SharedKotlinPair<SharedLibraryDrawableResource *, SharedLibraryDrawableResource *> *> *)getTabInfoIncludeDebug:(BOOL)includeDebug __attribute__((swift_name("getTabInfo(includeDebug:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsFilterCallbacks")))
@interface SharedEventsFilterCallbacks : SharedBase
- (instancetype)initWithOnAllEventsClicked:(void (^)(void))onAllEventsClicked onFavoriteEventsClicked:(void (^)(void))onFavoriteEventsClicked onDownloadedEventsClicked:(void (^)(void))onDownloadedEventsClicked __attribute__((swift_name("init(onAllEventsClicked:onFavoriteEventsClicked:onDownloadedEventsClicked:)"))) __attribute__((objc_designated_initializer));
- (SharedEventsFilterCallbacks *)doCopyOnAllEventsClicked:(void (^)(void))onAllEventsClicked onFavoriteEventsClicked:(void (^)(void))onFavoriteEventsClicked onDownloadedEventsClicked:(void (^)(void))onDownloadedEventsClicked __attribute__((swift_name("doCopy(onAllEventsClicked:onFavoriteEventsClicked:onDownloadedEventsClicked:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) void (^onAllEventsClicked)(void) __attribute__((swift_name("onAllEventsClicked")));
@property (readonly) void (^onDownloadedEventsClicked)(void) __attribute__((swift_name("onDownloadedEventsClicked")));
@property (readonly) void (^onFavoriteEventsClicked)(void) __attribute__((swift_name("onFavoriteEventsClicked")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsFilterState")))
@interface SharedEventsFilterState : SharedBase
- (instancetype)initWithStarredSelected:(BOOL)starredSelected downloadedSelected:(BOOL)downloadedSelected __attribute__((swift_name("init(starredSelected:downloadedSelected:)"))) __attribute__((objc_designated_initializer));
- (SharedEventsFilterState *)doCopyStarredSelected:(BOOL)starredSelected downloadedSelected:(BOOL)downloadedSelected __attribute__((swift_name("doCopy(starredSelected:downloadedSelected:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL downloadedSelected __attribute__((swift_name("downloadedSelected")));
@property (readonly) BOOL starredSelected __attribute__((swift_name("starredSelected")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedColorFamily")))
@interface SharedSharedColorFamily : SharedBase
- (instancetype)initWithColor:(uint64_t)color onColor:(uint64_t)onColor colorContainer:(uint64_t)colorContainer onColorContainer:(uint64_t)onColorContainer __attribute__((swift_name("init(color:onColor:colorContainer:onColorContainer:)"))) __attribute__((objc_designated_initializer));
- (SharedSharedColorFamily *)doCopyColor:(uint64_t)color onColor:(uint64_t)onColor colorContainer:(uint64_t)colorContainer onColorContainer:(uint64_t)onColorContainer __attribute__((swift_name("doCopy(color:onColor:colorContainer:onColorContainer:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) uint64_t color __attribute__((swift_name("color")));
@property (readonly) uint64_t colorContainer __attribute__((swift_name("colorContainer")));
@property (readonly) uint64_t onColor __attribute__((swift_name("onColor")));
@property (readonly) uint64_t onColorContainer __attribute__((swift_name("onColorContainer")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedExtendedColorScheme")))
@interface SharedSharedExtendedColorScheme : SharedBase
- (instancetype)initWithQuaternary:(SharedSharedColorFamily *)quaternary quinary:(SharedSharedColorFamily *)quinary __attribute__((swift_name("init(quaternary:quinary:)"))) __attribute__((objc_designated_initializer));
- (SharedSharedExtendedColorScheme *)doCopyQuaternary:(SharedSharedColorFamily *)quaternary quinary:(SharedSharedColorFamily *)quinary __attribute__((swift_name("doCopy(quaternary:quinary:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedSharedColorFamily *quaternary __attribute__((swift_name("quaternary")));
@property (readonly) SharedSharedColorFamily *quinary __attribute__((swift_name("quinary")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventState_")))
@interface SharedEventState_ : SharedBase
- (instancetype)initWithEventStatus:(SharedIWWWEventStatus *)eventStatus progression:(double)progression isInArea:(BOOL)isInArea endDateTime:(SharedKotlinInstant * _Nullable)endDateTime isSimulationModeEnabled:(BOOL)isSimulationModeEnabled __attribute__((swift_name("init(eventStatus:progression:isInArea:endDateTime:isSimulationModeEnabled:)"))) __attribute__((objc_designated_initializer));
- (SharedEventState_ *)doCopyEventStatus:(SharedIWWWEventStatus *)eventStatus progression:(double)progression isInArea:(BOOL)isInArea endDateTime:(SharedKotlinInstant * _Nullable)endDateTime isSimulationModeEnabled:(BOOL)isSimulationModeEnabled __attribute__((swift_name("doCopy(eventStatus:progression:isInArea:endDateTime:isSimulationModeEnabled:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinInstant * _Nullable endDateTime __attribute__((swift_name("endDateTime")));
@property (readonly) SharedIWWWEventStatus *eventStatus __attribute__((swift_name("eventStatus")));
@property (readonly) BOOL isInArea __attribute__((swift_name("isInArea")));
@property (readonly) BOOL isSimulationModeEnabled __attribute__((swift_name("isSimulationModeEnabled")));
@property (readonly) double progression __attribute__((swift_name("progression")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ByteArrayReader")))
@interface SharedByteArrayReader : SharedBase
- (instancetype)initWithBytes:(SharedKotlinByteArray *)bytes __attribute__((swift_name("init(bytes:)"))) __attribute__((objc_designated_initializer));
- (int32_t)readInt16 __attribute__((swift_name("readInt16()")));
- (int32_t)readInt32 __attribute__((swift_name("readInt32()")));
- (NSString *)readStringLength:(int32_t)length __attribute__((swift_name("readString(length:)")));
- (int32_t)readUInt8 __attribute__((swift_name("readUInt8()")));
- (int64_t)readVariableLengthQuantity __attribute__((swift_name("readVariableLengthQuantity()")));
- (void)skipCount:(int32_t)count __attribute__((swift_name("skip(count:)")));
@property int32_t position __attribute__((swift_name("position")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineScope")))
@protocol SharedKotlinx_coroutines_coreCoroutineScope
@required
@property (readonly) id<SharedKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CloseableCoroutineScope")))
@interface SharedCloseableCoroutineScope : SharedBase <SharedKotlinx_coroutines_coreCoroutineScope>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)close __attribute__((swift_name("close()")));
- (void)registerForCleanupAction:(void (^)(void))action __attribute__((swift_name("registerForCleanup(action:)")));
@property (readonly) id<SharedKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

__attribute__((swift_name("ImageResolver")))
@protocol SharedImageResolver
@required
- (id _Nullable)resolvePath:(NSString *)path __attribute__((swift_name("resolve(path:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSImageResolver")))
@interface SharedIOSImageResolver : SharedBase <SharedImageResolver>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (UIImage * _Nullable)resolvePath:(NSString *)path __attribute__((swift_name("resolve(path:)")));
- (UIImage * _Nullable)resolveFramePath:(NSString *)path frameIndex:(int32_t)frameIndex frameWidth:(int32_t)frameWidth frameHeight:(int32_t)frameHeight frameCount:(int32_t)frameCount __attribute__((swift_name("resolveFrame(path:frameIndex:frameWidth:frameHeight:frameCount:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Log")))
@interface SharedLog : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)log __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLog *shared __attribute__((swift_name("shared")));
- (void)dTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("d(tag:message:throwable:)")));
- (void)eTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("e(tag:message:throwable:)")));
- (void)iTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("i(tag:message:throwable:)")));
- (void)performanceTag:(NSString *)tag message:(NSString *)message __attribute__((swift_name("performance(tag:message:)")));
- (void)vTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("v(tag:message:throwable:)")));
- (void)wTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("w(tag:message:throwable:)")));
- (void)wtfTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("wtf(tag:message:throwable:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LogConfig")))
@interface SharedLogConfig : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)logConfig __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLogConfig *shared __attribute__((swift_name("shared")));
@property (readonly) BOOL ENABLE_DEBUG_LOGGING __attribute__((swift_name("ENABLE_DEBUG_LOGGING")));
@property (readonly) BOOL ENABLE_PERFORMANCE_LOGGING __attribute__((swift_name("ENABLE_PERFORMANCE_LOGGING")));
@property (readonly) BOOL ENABLE_VERBOSE_LOGGING __attribute__((swift_name("ENABLE_VERBOSE_LOGGING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWLogger")))
@interface SharedWWWLogger : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)wWWLogger __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWWWLogger *shared __attribute__((swift_name("shared")));
- (void)dTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("d(tag:message:throwable:)")));
- (void)eTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("e(tag:message:throwable:)")));
- (void)iTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("i(tag:message:throwable:)")));
- (void)vTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("v(tag:message:throwable:)")));
- (void)wTag:(NSString *)tag message:(NSString *)message throwable:(SharedKotlinThrowable * _Nullable)throwable __attribute__((swift_name("w(tag:message:throwable:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WaveProgressionObserver")))
@interface SharedWaveProgressionObserver : SharedBase
- (instancetype)initWithScope:(id<SharedKotlinx_coroutines_coreCoroutineScope>)scope eventMap:(SharedAbstractEventMap<id> *)eventMap event:(id<SharedIWWWEvent> _Nullable)event __attribute__((swift_name("init(scope:eventMap:event:)"))) __attribute__((objc_designated_initializer));
- (void)pauseObservation __attribute__((swift_name("pauseObservation()")));
- (void)startObservation __attribute__((swift_name("startObservation()")));
- (void)stopObservation __attribute__((swift_name("stopObservation()")));
@end

__attribute__((swift_name("IMapDownloadManager")))
@protocol SharedIMapDownloadManager
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cancelDownloadWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelDownload(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)checkIfMapIsAvailableMapId:(NSString *)mapId autoDownload:(BOOL)autoDownload completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("checkIfMapIsAvailable(mapId:autoDownload:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)downloadMapMapId:(NSString *)mapId onMapDownloaded:(void (^ _Nullable)(void))onMapDownloaded completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("downloadMap(mapId:onMapDownloaded:completionHandler:)")));
- (NSString *)getErrorMessageErrorCode:(int32_t)errorCode __attribute__((swift_name("getErrorMessage(errorCode:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> featureState __attribute__((swift_name("featureState")));
@end

__attribute__((swift_name("BaseMapDownloadViewModel")))
@interface SharedBaseMapDownloadViewModel : SharedBaseViewModel <SharedIMapDownloadManager>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SharedBaseMapDownloadViewModelCompanion *companion __attribute__((swift_name("companion")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cancelDownloadWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelDownload(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)cancelPlatformDownloadWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cancelPlatformDownload(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)checkIfMapIsAvailableMapId:(NSString *)mapId autoDownload:(BOOL)autoDownload completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("checkIfMapIsAvailable(mapId:autoDownload:completionHandler:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)clearCacheForInstalledMapsMapIds:(NSArray<NSString *> *)mapIds __attribute__((swift_name("clearCacheForInstalledMaps(mapIds:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)downloadMapMapId:(NSString *)mapId onMapDownloaded:(void (^ _Nullable)(void))onMapDownloaded completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("downloadMap(mapId:onMapDownloaded:completionHandler:)")));
- (NSString *)getErrorMessageErrorCode:(int32_t)errorCode __attribute__((swift_name("getErrorMessage(errorCode:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)getLocalizedErrorMessageErrorCode:(int32_t)errorCode __attribute__((swift_name("getLocalizedErrorMessage(errorCode:)")));
- (void)handleDownloadCancellation __attribute__((swift_name("handleDownloadCancellation()")));
- (void)handleDownloadFailureErrorCode:(int32_t)errorCode shouldRetry:(BOOL)shouldRetry __attribute__((swift_name("handleDownloadFailure(errorCode:shouldRetry:)")));
- (void)handleDownloadProgressTotalBytes:(int64_t)totalBytes downloadedBytes:(int64_t)downloadedBytes __attribute__((swift_name("handleDownloadProgress(totalBytes:downloadedBytes:)")));
- (void)handleDownloadSuccess __attribute__((swift_name("handleDownloadSuccess()")));
- (void)handleInstallCompleteModuleIds:(NSArray<NSString *> *)moduleIds __attribute__((swift_name("handleInstallComplete(moduleIds:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)isMapInstalledMapId:(NSString *)mapId completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isMapInstalled(mapId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)startPlatformDownloadMapId:(NSString *)mapId onMapDownloaded:(void (^ _Nullable)(void))onMapDownloaded completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("startPlatformDownload(mapId:onMapDownloaded:completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreMutableStateFlow> _featureState __attribute__((swift_name("_featureState")));
@property NSString * _Nullable currentMapId __attribute__((swift_name("currentMapId")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> featureState __attribute__((swift_name("featureState")));
@property (readonly) SharedMapDownloadUtilsRetryManager *retryManager __attribute__((swift_name("retryManager")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BaseMapDownloadViewModel.Companion")))
@interface SharedBaseMapDownloadViewModelCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBaseMapDownloadViewModelCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsViewModel")))
@interface SharedEventsViewModel : SharedBaseViewModel
- (instancetype)initWithEventsRepository:(id<SharedEventsRepository>)eventsRepository getSortedEventsUseCase:(SharedGetSortedEventsUseCase *)getSortedEventsUseCase filterEventsUseCase:(SharedFilterEventsUseCase *)filterEventsUseCase checkEventFavoritesUseCase:(SharedCheckEventFavoritesUseCase *)checkEventFavoritesUseCase platform:(SharedWWWPlatform *)platform __attribute__((swift_name("init(eventsRepository:getSortedEventsUseCase:filterEventsUseCase:checkEventFavoritesUseCase:platform:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedEventsViewModelCompanion *companion __attribute__((swift_name("companion")));
- (void)filterEventsOnlyFavorites:(BOOL)onlyFavorites onlyDownloaded:(BOOL)onlyDownloaded __attribute__((swift_name("filterEvents(onlyFavorites:onlyDownloaded:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> events __attribute__((swift_name("events")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> hasFavorites __attribute__((swift_name("hasFavorites")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> hasLoadingError __attribute__((swift_name("hasLoadingError")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isLoading __attribute__((swift_name("isLoading")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsViewModel.Companion")))
@interface SharedEventsViewModelCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEventsViewModelCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapDownloadUtils")))
@interface SharedMapDownloadUtils : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mapDownloadUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapDownloadUtils *shared __attribute__((swift_name("shared")));
- (int32_t)calculateProgressPercentTotalBytes:(int64_t)totalBytes downloadedBytes:(int64_t)downloadedBytes __attribute__((swift_name("calculateProgressPercent(totalBytes:downloadedBytes:)")));
- (BOOL)isActiveDownloadState:(SharedMapFeatureState *)state __attribute__((swift_name("isActiveDownload(state:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapDownloadUtils.RetryManager")))
@interface SharedMapDownloadUtilsRetryManager : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SharedMapDownloadUtilsRetryManagerCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)canRetry __attribute__((swift_name("canRetry()")));
- (int32_t)getCurrentRetryCount __attribute__((swift_name("getCurrentRetryCount()")));
- (int64_t)getNextRetryDelay __attribute__((swift_name("getNextRetryDelay()")));
- (int32_t)incrementRetryCount __attribute__((swift_name("incrementRetryCount()")));
- (void)resetRetryCount __attribute__((swift_name("resetRetryCount()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapDownloadUtils.RetryManagerCompanion")))
@interface SharedMapDownloadUtilsRetryManagerCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapDownloadUtilsRetryManagerCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int64_t BASE_RETRY_DELAY_MS __attribute__((swift_name("BASE_RETRY_DELAY_MS")));
@property (readonly) int32_t MAX_RETRIES __attribute__((swift_name("MAX_RETRIES")));
@end

@interface SharedPolygon (Extensions)
- (SharedPolygon *)close __attribute__((swift_name("close()")));
- (SharedPolygon *)inverted __attribute__((swift_name("inverted()")));
- (SharedPolygon *)move __attribute__((swift_name("move()")));
- (SharedPolygon *)plusOther:(SharedPolygon *)other __attribute__((swift_name("plus(other:)")));
- (SharedPolygon *)subListStart:(SharedPosition *)start lastId:(int32_t)lastId __attribute__((swift_name("subList(start:lastId:)")));
- (SharedPolygon *)withoutLastN:(int32_t)n __attribute__((swift_name("withoutLast(n:)")));
- (SharedPolygon *)xferFromPolygon:(SharedPolygon *)polygon __attribute__((swift_name("xferFrom(polygon:)")));
@end

@interface SharedRes (Extensions)
@property (readonly) NSDictionary<NSString *, SharedLibraryDrawableResource *> *allDrawableResources __attribute__((swift_name("allDrawableResources")));
@property (readonly) NSDictionary<NSString *, SharedLibraryFontResource *> *allFontResources __attribute__((swift_name("allFontResources")));
@property (readonly) NSDictionary<NSString *, SharedLibraryPluralStringResource *> *allPluralStringResources __attribute__((swift_name("allPluralStringResources")));
@property (readonly) NSDictionary<NSString *, SharedLibraryStringArrayResource *> *allStringArrayResources __attribute__((swift_name("allStringArrayResources")));
@property (readonly) NSDictionary<NSString *, SharedLibraryStringResource *> *allStringResources __attribute__((swift_name("allStringResources")));
@end

@interface SharedResDrawable (Extensions)
@property (readonly) SharedLibraryDrawableResource *about_icon __attribute__((swift_name("about_icon")));
@property (readonly) SharedLibraryDrawableResource *about_icon_selected __attribute__((swift_name("about_icon_selected")));
@property (readonly) SharedLibraryDrawableResource *background __attribute__((swift_name("background")));
@property (readonly) SharedLibraryDrawableResource *debug_icon __attribute__((swift_name("debug_icon")));
@property (readonly) SharedLibraryDrawableResource *debug_icon_selected __attribute__((swift_name("debug_icon_selected")));
@property (readonly) SharedLibraryDrawableResource *downloaded_icon __attribute__((swift_name("downloaded_icon")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_hit __attribute__((swift_name("e_choreography_hit")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_waiting __attribute__((swift_name("e_choreography_waiting")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_1 __attribute__((swift_name("e_choreography_warming_seq_1")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_2 __attribute__((swift_name("e_choreography_warming_seq_2")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_3 __attribute__((swift_name("e_choreography_warming_seq_3")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_4 __attribute__((swift_name("e_choreography_warming_seq_4")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_5 __attribute__((swift_name("e_choreography_warming_seq_5")));
@property (readonly) SharedLibraryDrawableResource *e_choreography_warming_seq_6 __attribute__((swift_name("e_choreography_warming_seq_6")));
@property (readonly) SharedLibraryDrawableResource *e_community_africa __attribute__((swift_name("e_community_africa")));
@property (readonly) SharedLibraryDrawableResource *e_community_asia __attribute__((swift_name("e_community_asia")));
@property (readonly) SharedLibraryDrawableResource *e_community_europe __attribute__((swift_name("e_community_europe")));
@property (readonly) SharedLibraryDrawableResource *e_community_middle_east __attribute__((swift_name("e_community_middle_east")));
@property (readonly) SharedLibraryDrawableResource *e_community_north_america __attribute__((swift_name("e_community_north_america")));
@property (readonly) SharedLibraryDrawableResource *e_community_oceania __attribute__((swift_name("e_community_oceania")));
@property (readonly) SharedLibraryDrawableResource *e_community_south_america __attribute__((swift_name("e_community_south_america")));
@property (readonly) SharedLibraryDrawableResource *e_country_argentina __attribute__((swift_name("e_country_argentina")));
@property (readonly) SharedLibraryDrawableResource *e_country_australia __attribute__((swift_name("e_country_australia")));
@property (readonly) SharedLibraryDrawableResource *e_country_brazil __attribute__((swift_name("e_country_brazil")));
@property (readonly) SharedLibraryDrawableResource *e_country_canada __attribute__((swift_name("e_country_canada")));
@property (readonly) SharedLibraryDrawableResource *e_country_chile __attribute__((swift_name("e_country_chile")));
@property (readonly) SharedLibraryDrawableResource *e_country_china __attribute__((swift_name("e_country_china")));
@property (readonly) SharedLibraryDrawableResource *e_country_colombia __attribute__((swift_name("e_country_colombia")));
@property (readonly) SharedLibraryDrawableResource *e_country_democratic_republic_of_the_congo __attribute__((swift_name("e_country_democratic_republic_of_the_congo")));
@property (readonly) SharedLibraryDrawableResource *e_country_egypt __attribute__((swift_name("e_country_egypt")));
@property (readonly) SharedLibraryDrawableResource *e_country_england __attribute__((swift_name("e_country_england")));
@property (readonly) SharedLibraryDrawableResource *e_country_france __attribute__((swift_name("e_country_france")));
@property (readonly) SharedLibraryDrawableResource *e_country_germany __attribute__((swift_name("e_country_germany")));
@property (readonly) SharedLibraryDrawableResource *e_country_india __attribute__((swift_name("e_country_india")));
@property (readonly) SharedLibraryDrawableResource *e_country_indonesia __attribute__((swift_name("e_country_indonesia")));
@property (readonly) SharedLibraryDrawableResource *e_country_iran __attribute__((swift_name("e_country_iran")));
@property (readonly) SharedLibraryDrawableResource *e_country_italy __attribute__((swift_name("e_country_italy")));
@property (readonly) SharedLibraryDrawableResource *e_country_japan __attribute__((swift_name("e_country_japan")));
@property (readonly) SharedLibraryDrawableResource *e_country_kenya __attribute__((swift_name("e_country_kenya")));
@property (readonly) SharedLibraryDrawableResource *e_country_mexico __attribute__((swift_name("e_country_mexico")));
@property (readonly) SharedLibraryDrawableResource *e_country_nigeria __attribute__((swift_name("e_country_nigeria")));
@property (readonly) SharedLibraryDrawableResource *e_country_pakistan __attribute__((swift_name("e_country_pakistan")));
@property (readonly) SharedLibraryDrawableResource *e_country_peru __attribute__((swift_name("e_country_peru")));
@property (readonly) SharedLibraryDrawableResource *e_country_philippines __attribute__((swift_name("e_country_philippines")));
@property (readonly) SharedLibraryDrawableResource *e_country_russia __attribute__((swift_name("e_country_russia")));
@property (readonly) SharedLibraryDrawableResource *e_country_south_africa __attribute__((swift_name("e_country_south_africa")));
@property (readonly) SharedLibraryDrawableResource *e_country_south_korea __attribute__((swift_name("e_country_south_korea")));
@property (readonly) SharedLibraryDrawableResource *e_country_spain __attribute__((swift_name("e_country_spain")));
@property (readonly) SharedLibraryDrawableResource *e_country_thailand __attribute__((swift_name("e_country_thailand")));
@property (readonly) SharedLibraryDrawableResource *e_country_turkey __attribute__((swift_name("e_country_turkey")));
@property (readonly) SharedLibraryDrawableResource *e_country_united_arab_emirates __attribute__((swift_name("e_country_united_arab_emirates")));
@property (readonly) SharedLibraryDrawableResource *e_country_usa __attribute__((swift_name("e_country_usa")));
@property (readonly) SharedLibraryDrawableResource *e_location_bangalore_india __attribute__((swift_name("e_location_bangalore_india")));
@property (readonly) SharedLibraryDrawableResource *e_location_bangkok_thailand __attribute__((swift_name("e_location_bangkok_thailand")));
@property (readonly) SharedLibraryDrawableResource *e_location_beijing_china __attribute__((swift_name("e_location_beijing_china")));
@property (readonly) SharedLibraryDrawableResource *e_location_berlin_germany __attribute__((swift_name("e_location_berlin_germany")));
@property (readonly) SharedLibraryDrawableResource *e_location_bogota_colombia __attribute__((swift_name("e_location_bogota_colombia")));
@property (readonly) SharedLibraryDrawableResource *e_location_buenos_aires_argentina __attribute__((swift_name("e_location_buenos_aires_argentina")));
@property (readonly) SharedLibraryDrawableResource *e_location_cairo_egypt __attribute__((swift_name("e_location_cairo_egypt")));
@property (readonly) SharedLibraryDrawableResource *e_location_chicago_usa __attribute__((swift_name("e_location_chicago_usa")));
@property (readonly) SharedLibraryDrawableResource *e_location_delhi_india __attribute__((swift_name("e_location_delhi_india")));
@property (readonly) SharedLibraryDrawableResource *e_location_dubai_united_arab_emirates __attribute__((swift_name("e_location_dubai_united_arab_emirates")));
@property (readonly) SharedLibraryDrawableResource *e_location_hong_kong_china __attribute__((swift_name("e_location_hong_kong_china")));
@property (readonly) SharedLibraryDrawableResource *e_location_istanbul_turkey __attribute__((swift_name("e_location_istanbul_turkey")));
@property (readonly) SharedLibraryDrawableResource *e_location_jakarta_indonesia __attribute__((swift_name("e_location_jakarta_indonesia")));
@property (readonly) SharedLibraryDrawableResource *e_location_johannesburg_south_africa __attribute__((swift_name("e_location_johannesburg_south_africa")));
@property (readonly) SharedLibraryDrawableResource *e_location_karachi_pakistan __attribute__((swift_name("e_location_karachi_pakistan")));
@property (readonly) SharedLibraryDrawableResource *e_location_kinshasa_democratic_republic_of_the_congo __attribute__((swift_name("e_location_kinshasa_democratic_republic_of_the_congo")));
@property (readonly) SharedLibraryDrawableResource *e_location_lagos_nigeria __attribute__((swift_name("e_location_lagos_nigeria")));
@property (readonly) SharedLibraryDrawableResource *e_location_lima_peru __attribute__((swift_name("e_location_lima_peru")));
@property (readonly) SharedLibraryDrawableResource *e_location_london_england __attribute__((swift_name("e_location_london_england")));
@property (readonly) SharedLibraryDrawableResource *e_location_los_angeles_usa __attribute__((swift_name("e_location_los_angeles_usa")));
@property (readonly) SharedLibraryDrawableResource *e_location_madrid_spain __attribute__((swift_name("e_location_madrid_spain")));
@property (readonly) SharedLibraryDrawableResource *e_location_manila_philippines __attribute__((swift_name("e_location_manila_philippines")));
@property (readonly) SharedLibraryDrawableResource *e_location_melbourne_australia __attribute__((swift_name("e_location_melbourne_australia")));
@property (readonly) SharedLibraryDrawableResource *e_location_mexico_city_mexico __attribute__((swift_name("e_location_mexico_city_mexico")));
@property (readonly) SharedLibraryDrawableResource *e_location_moscow_russia __attribute__((swift_name("e_location_moscow_russia")));
@property (readonly) SharedLibraryDrawableResource *e_location_mumbai_india __attribute__((swift_name("e_location_mumbai_india")));
@property (readonly) SharedLibraryDrawableResource *e_location_nairobi_kenya __attribute__((swift_name("e_location_nairobi_kenya")));
@property (readonly) SharedLibraryDrawableResource *e_location_new_york_usa __attribute__((swift_name("e_location_new_york_usa")));
@property (readonly) SharedLibraryDrawableResource *e_location_paris_france __attribute__((swift_name("e_location_paris_france")));
@property (readonly) SharedLibraryDrawableResource *e_location_rome_italy __attribute__((swift_name("e_location_rome_italy")));
@property (readonly) SharedLibraryDrawableResource *e_location_san_francisco_usa __attribute__((swift_name("e_location_san_francisco_usa")));
@property (readonly) SharedLibraryDrawableResource *e_location_santiago_chile __attribute__((swift_name("e_location_santiago_chile")));
@property (readonly) SharedLibraryDrawableResource *e_location_sao_paulo_brazil __attribute__((swift_name("e_location_sao_paulo_brazil")));
@property (readonly) SharedLibraryDrawableResource *e_location_seoul_south_korea __attribute__((swift_name("e_location_seoul_south_korea")));
@property (readonly) SharedLibraryDrawableResource *e_location_shanghai_china __attribute__((swift_name("e_location_shanghai_china")));
@property (readonly) SharedLibraryDrawableResource *e_location_sydney_australia __attribute__((swift_name("e_location_sydney_australia")));
@property (readonly) SharedLibraryDrawableResource *e_location_tehran_iran __attribute__((swift_name("e_location_tehran_iran")));
@property (readonly) SharedLibraryDrawableResource *e_location_tokyo_japan __attribute__((swift_name("e_location_tokyo_japan")));
@property (readonly) SharedLibraryDrawableResource *e_location_toronto_canada __attribute__((swift_name("e_location_toronto_canada")));
@property (readonly) SharedLibraryDrawableResource *e_location_vancouver_canada __attribute__((swift_name("e_location_vancouver_canada")));
@property (readonly) SharedLibraryDrawableResource *e_map_bangalore_india __attribute__((swift_name("e_map_bangalore_india")));
@property (readonly) SharedLibraryDrawableResource *e_map_bangkok_thailand __attribute__((swift_name("e_map_bangkok_thailand")));
@property (readonly) SharedLibraryDrawableResource *e_map_beijing_china __attribute__((swift_name("e_map_beijing_china")));
@property (readonly) SharedLibraryDrawableResource *e_map_berlin_germany __attribute__((swift_name("e_map_berlin_germany")));
@property (readonly) SharedLibraryDrawableResource *e_map_bogota_colombia __attribute__((swift_name("e_map_bogota_colombia")));
@property (readonly) SharedLibraryDrawableResource *e_map_buenos_aires_argentina __attribute__((swift_name("e_map_buenos_aires_argentina")));
@property (readonly) SharedLibraryDrawableResource *e_map_cairo_egypt __attribute__((swift_name("e_map_cairo_egypt")));
@property (readonly) SharedLibraryDrawableResource *e_map_chicago_usa __attribute__((swift_name("e_map_chicago_usa")));
@property (readonly) SharedLibraryDrawableResource *e_map_delhi_india __attribute__((swift_name("e_map_delhi_india")));
@property (readonly) SharedLibraryDrawableResource *e_map_dubai_united_arab_emirates __attribute__((swift_name("e_map_dubai_united_arab_emirates")));
@property (readonly) SharedLibraryDrawableResource *e_map_hong_kong_china __attribute__((swift_name("e_map_hong_kong_china")));
@property (readonly) SharedLibraryDrawableResource *e_map_istanbul_turkey __attribute__((swift_name("e_map_istanbul_turkey")));
@property (readonly) SharedLibraryDrawableResource *e_map_jakarta_indonesia __attribute__((swift_name("e_map_jakarta_indonesia")));
@property (readonly) SharedLibraryDrawableResource *e_map_johannesburg_south_africa __attribute__((swift_name("e_map_johannesburg_south_africa")));
@property (readonly) SharedLibraryDrawableResource *e_map_karachi_pakistan __attribute__((swift_name("e_map_karachi_pakistan")));
@property (readonly) SharedLibraryDrawableResource *e_map_kinshasa_democratic_republic_of_the_congo __attribute__((swift_name("e_map_kinshasa_democratic_republic_of_the_congo")));
@property (readonly) SharedLibraryDrawableResource *e_map_lagos_nigeria __attribute__((swift_name("e_map_lagos_nigeria")));
@property (readonly) SharedLibraryDrawableResource *e_map_lima_peru __attribute__((swift_name("e_map_lima_peru")));
@property (readonly) SharedLibraryDrawableResource *e_map_london_england __attribute__((swift_name("e_map_london_england")));
@property (readonly) SharedLibraryDrawableResource *e_map_los_angeles_usa __attribute__((swift_name("e_map_los_angeles_usa")));
@property (readonly) SharedLibraryDrawableResource *e_map_madrid_spain __attribute__((swift_name("e_map_madrid_spain")));
@property (readonly) SharedLibraryDrawableResource *e_map_manila_philippines __attribute__((swift_name("e_map_manila_philippines")));
@property (readonly) SharedLibraryDrawableResource *e_map_melbourne_australia __attribute__((swift_name("e_map_melbourne_australia")));
@property (readonly) SharedLibraryDrawableResource *e_map_mexico_city_mexico __attribute__((swift_name("e_map_mexico_city_mexico")));
@property (readonly) SharedLibraryDrawableResource *e_map_moscow_russia __attribute__((swift_name("e_map_moscow_russia")));
@property (readonly) SharedLibraryDrawableResource *e_map_mumbai_india __attribute__((swift_name("e_map_mumbai_india")));
@property (readonly) SharedLibraryDrawableResource *e_map_nairobi_kenya __attribute__((swift_name("e_map_nairobi_kenya")));
@property (readonly) SharedLibraryDrawableResource *e_map_new_york_usa __attribute__((swift_name("e_map_new_york_usa")));
@property (readonly) SharedLibraryDrawableResource *e_map_paris_france __attribute__((swift_name("e_map_paris_france")));
@property (readonly) SharedLibraryDrawableResource *e_map_rome_italy __attribute__((swift_name("e_map_rome_italy")));
@property (readonly) SharedLibraryDrawableResource *e_map_san_francisco_usa __attribute__((swift_name("e_map_san_francisco_usa")));
@property (readonly) SharedLibraryDrawableResource *e_map_santiago_chile __attribute__((swift_name("e_map_santiago_chile")));
@property (readonly) SharedLibraryDrawableResource *e_map_sao_paulo_brazil __attribute__((swift_name("e_map_sao_paulo_brazil")));
@property (readonly) SharedLibraryDrawableResource *e_map_seoul_south_korea __attribute__((swift_name("e_map_seoul_south_korea")));
@property (readonly) SharedLibraryDrawableResource *e_map_shanghai_china __attribute__((swift_name("e_map_shanghai_china")));
@property (readonly) SharedLibraryDrawableResource *e_map_sydney_australia __attribute__((swift_name("e_map_sydney_australia")));
@property (readonly) SharedLibraryDrawableResource *e_map_tehran_iran __attribute__((swift_name("e_map_tehran_iran")));
@property (readonly) SharedLibraryDrawableResource *e_map_tokyo_japan __attribute__((swift_name("e_map_tokyo_japan")));
@property (readonly) SharedLibraryDrawableResource *e_map_toronto_canada __attribute__((swift_name("e_map_toronto_canada")));
@property (readonly) SharedLibraryDrawableResource *e_map_vancouver_canada __attribute__((swift_name("e_map_vancouver_canada")));
@property (readonly) SharedLibraryDrawableResource *event_done __attribute__((swift_name("event_done")));
@property (readonly) SharedLibraryDrawableResource *favorite_off __attribute__((swift_name("favorite_off")));
@property (readonly) SharedLibraryDrawableResource *favorite_on __attribute__((swift_name("favorite_on")));
@property (readonly) SharedLibraryDrawableResource *geoloc_refresh_icon __attribute__((swift_name("geoloc_refresh_icon")));
@property (readonly) SharedLibraryDrawableResource *instagram_icon __attribute__((swift_name("instagram_icon")));
@property (readonly) SharedLibraryDrawableResource *map_error __attribute__((swift_name("map_error")));
@property (readonly) SharedLibraryDrawableResource *not_found __attribute__((swift_name("not_found")));
@property (readonly) SharedLibraryDrawableResource *target_me_active __attribute__((swift_name("target_me_active")));
@property (readonly) SharedLibraryDrawableResource *target_me_inactive __attribute__((swift_name("target_me_inactive")));
@property (readonly) SharedLibraryDrawableResource *target_wave_active __attribute__((swift_name("target_wave_active")));
@property (readonly) SharedLibraryDrawableResource *target_wave_inactive __attribute__((swift_name("target_wave_inactive")));
@property (readonly) SharedLibraryDrawableResource *transparent __attribute__((swift_name("transparent")));
@property (readonly) SharedLibraryDrawableResource *waves_icon __attribute__((swift_name("waves_icon")));
@property (readonly) SharedLibraryDrawableResource *waves_icon_selected __attribute__((swift_name("waves_icon_selected")));
@property (readonly) SharedLibraryDrawableResource *www_logo_transparent __attribute__((swift_name("www_logo_transparent")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoreographyResourcesKt")))
@interface SharedChoreographyResourcesKt : SharedBase
+ (SharedResourcesStringResource *)getChoreographyHitText __attribute__((swift_name("getChoreographyHitText()")));
+ (SharedResourcesStringResource *)getChoreographyTextSequenceType:(NSString *)sequenceType sequenceNumber:(SharedInt * _Nullable)sequenceNumber __attribute__((swift_name("getChoreographyText(sequenceType:sequenceNumber:)")));
+ (SharedResourcesStringResource *)getChoreographyWaitingText __attribute__((swift_name("getChoreographyWaitingText()")));
+ (SharedResourcesStringResource *)getChoreographyWarmingTextSeq:(SharedInt * _Nullable)seq __attribute__((swift_name("getChoreographyWarmingText(seq:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CommonModuleKt")))
@interface SharedCommonModuleKt : SharedBase
@property (class, readonly) SharedKoin_coreModule *commonModule __attribute__((swift_name("commonModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataStore_iosKt")))
@interface SharedDataStore_iosKt : SharedBase
+ (NSString *)keyValueStorePath __attribute__((swift_name("keyValueStorePath()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DataStoreKt")))
@interface SharedDataStoreKt : SharedBase

/**
 * @note annotations
 *   androidx.annotation.VisibleForTesting
*/
+ (id<SharedDatastore_coreDataStore>)createDataStoreProducePath:(NSString *(^)(void))producePath __attribute__((swift_name("createDataStore(producePath:)"))) __attribute__((deprecated("Tests should use testDatastoreModule with TestDataStoreFactory instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DatastoreModuleKt")))
@interface SharedDatastoreModuleKt : SharedBase
@property (class, readonly) SharedKoin_coreModule *datastoreModule __attribute__((swift_name("datastoreModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DurationFormattingKt")))
@interface SharedDurationFormattingKt : SharedBase
+ (NSString *)formatDurationDuration:(int64_t)duration __attribute__((swift_name("formatDuration(duration:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventsResourcesKt")))
@interface SharedEventsResourcesKt : SharedBase
+ (SharedResourcesStringResource *)getCommunityTextId:(NSString * _Nullable)id __attribute__((swift_name("getCommunityText(id:)")));
+ (SharedResourcesStringResource *)getCountryTextId:(NSString * _Nullable)id __attribute__((swift_name("getCountryText(id:)")));
+ (id _Nullable)getEventImageType:(NSString *)type id:(NSString *)id __attribute__((swift_name("getEventImage(type:id:)")));
+ (SharedResourcesStringResource *)getEventTextType:(NSString *)type id:(NSString *)id __attribute__((swift_name("getEventText(type:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HelperKt")))
@interface SharedHelperKt : SharedBase
+ (void)doInitKoin __attribute__((swift_name("doInitKoin()")));
+ (void)doInitKoin_ __attribute__((swift_name("doInitKoin_()"))) __attribute__((deprecated("Renamed to doInitKoin() to match Swift side.")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HelpersKt")))
@interface SharedHelpersKt : SharedBase
+ (void)updateIfChanged:(id<SharedKotlinx_coroutines_coreMutableStateFlow>)receiver newValue:(id _Nullable)newValue __attribute__((swift_name("updateIfChanged(_:newValue:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HelpersModuleKt")))
@interface SharedHelpersModuleKt : SharedBase
@property (class, readonly) SharedKoin_coreModule *helpersModule __attribute__((swift_name("helpersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSModuleKt")))
@interface SharedIOSModuleKt : SharedBase
@property (class, readonly) SharedKoin_coreModule *IOSModule __attribute__((swift_name("IOSModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSReactivePattern_iosKt")))
@interface SharedIOSReactivePattern_iosKt : SharedBase
+ (id<SharedIOSObservable>)toIOSObservable:(id<SharedKotlinx_coroutines_coreStateFlow>)receiver __attribute__((swift_name("toIOSObservable(_:)")));
+ (id<SharedIOSObservable>)toIOSObservableFlow:(id<SharedKotlinx_coroutines_coreFlow>)receiver __attribute__((swift_name("toIOSObservableFlow(_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InfoStringResourcesKt")))
@interface SharedInfoStringResourcesKt : SharedBase
@property (class, readonly) NSArray<SharedKotlinPair<SharedResourcesStringResource *, SharedResourcesStringResource *> *> *faq_contents __attribute__((swift_name("faq_contents")));
@property (class, readonly) NSArray<SharedResourcesStringResource *> *infos_core __attribute__((swift_name("infos_core")));
@property (class, readonly) NSDictionary<SharedResourcesStringResource *, NSArray<SharedResourcesStringResource *> *> *rules_hierarchy __attribute__((swift_name("rules_hierarchy")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LocationPermissions_iosKt")))
@interface SharedLocationPermissions_iosKt : SharedBase
+ (void)PlatformOpenLocationSettings __attribute__((swift_name("PlatformOpenLocationSettings()")));
+ (void)PlatformRequestLocationPermission __attribute__((swift_name("PlatformRequestLocationPermission()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MainViewControllerKt")))
@interface SharedMainViewControllerKt : SharedBase
+ (UIViewController *)MainViewController __attribute__((swift_name("MainViewController()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NapierInit_iosKt")))
@interface SharedNapierInit_iosKt : SharedBase
+ (void)doInitNapier __attribute__((swift_name("doInitNapier()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Platform_iosKt")))
@interface SharedPlatform_iosKt : SharedBase

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)cacheDeepFileFileName:(NSString *)fileName completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("cacheDeepFile(fileName:completionHandler:)")));
+ (NSString *)cacheStringToFileFileName:(NSString *)fileName content:(NSString *)content __attribute__((swift_name("cacheStringToFile(fileName:content:)")));
+ (BOOL)cachedFileExistsFileName:(NSString *)fileName __attribute__((swift_name("cachedFileExists(fileName:)")));
+ (NSString * _Nullable)cachedFilePathFileName:(NSString *)fileName __attribute__((swift_name("cachedFilePath(fileName:)")));
+ (void)clearEventCacheEventId:(NSString *)eventId __attribute__((swift_name("clearEventCache(eventId:)")));
+ (NSString *)getCacheDir __attribute__((swift_name("getCacheDir()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)getMapFileAbsolutePathEventId:(NSString *)eventId extension:(NSString *)extension completionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getMapFileAbsolutePath(eventId:extension:completionHandler:)")));
+ (void)doInitKoinIOS __attribute__((swift_name("doInitKoinIOS()")));
+ (BOOL)isCachedFileStaleFileName:(NSString *)fileName __attribute__((swift_name("isCachedFileStale(fileName:)")));
+ (NSString *)localizeStringResource:(SharedResourcesStringResource *)resource __attribute__((swift_name("localizeString(resource:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
+ (void)readGeoJsonEventId:(NSString *)eventId completionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("readGeoJson(eventId:completionHandler:)")));
+ (void)updateCacheMetadataFileName:(NSString *)fileName __attribute__((swift_name("updateCacheMetadata(fileName:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedColorsKt")))
@interface SharedSharedColorsKt : SharedBase
@property (class, readonly) SharedMaterial3ColorScheme *SharedLightColorScheme __attribute__((swift_name("SharedLightColorScheme")));
@property (class, readonly) uint64_t backgroundLight __attribute__((swift_name("backgroundLight")));
@property (class, readonly) uint64_t errorContainerLight __attribute__((swift_name("errorContainerLight")));
@property (class, readonly) uint64_t errorLight __attribute__((swift_name("errorLight")));
@property (class, readonly) uint64_t inverseOnSurfaceLight __attribute__((swift_name("inverseOnSurfaceLight")));
@property (class, readonly) uint64_t inversePrimaryLight __attribute__((swift_name("inversePrimaryLight")));
@property (class, readonly) uint64_t inverseSurfaceLight __attribute__((swift_name("inverseSurfaceLight")));
@property (class, readonly) uint64_t onBackgroundLight __attribute__((swift_name("onBackgroundLight")));
@property (class, readonly) uint64_t onErrorContainerLight __attribute__((swift_name("onErrorContainerLight")));
@property (class, readonly) uint64_t onErrorLight __attribute__((swift_name("onErrorLight")));
@property (class, readonly) uint64_t onPrimaryContainerLight __attribute__((swift_name("onPrimaryContainerLight")));
@property (class, readonly) uint64_t onPrimaryLight __attribute__((swift_name("onPrimaryLight")));
@property (class, readonly) uint64_t onQuaternaryContainerLight __attribute__((swift_name("onQuaternaryContainerLight")));
@property (class, readonly) uint64_t onQuaternaryLight __attribute__((swift_name("onQuaternaryLight")));
@property (class, readonly) uint64_t onQuinaryContainerLight __attribute__((swift_name("onQuinaryContainerLight")));
@property (class, readonly) uint64_t onQuinaryLight __attribute__((swift_name("onQuinaryLight")));
@property (class, readonly) uint64_t onSecondaryContainerLight __attribute__((swift_name("onSecondaryContainerLight")));
@property (class, readonly) uint64_t onSecondaryLight __attribute__((swift_name("onSecondaryLight")));
@property (class, readonly) uint64_t onSurfaceLight __attribute__((swift_name("onSurfaceLight")));
@property (class, readonly) uint64_t onSurfaceVariantLight __attribute__((swift_name("onSurfaceVariantLight")));
@property (class, readonly) uint64_t onTertiaryContainerLight __attribute__((swift_name("onTertiaryContainerLight")));
@property (class, readonly) uint64_t onTertiaryLight __attribute__((swift_name("onTertiaryLight")));
@property (class, readonly) uint64_t outlineLight __attribute__((swift_name("outlineLight")));
@property (class, readonly) uint64_t outlineVariantLight __attribute__((swift_name("outlineVariantLight")));
@property (class, readonly) uint64_t primaryContainerLight __attribute__((swift_name("primaryContainerLight")));
@property (class, readonly) uint64_t primaryLight __attribute__((swift_name("primaryLight")));
@property (class, readonly) uint64_t quaternaryContainerLight __attribute__((swift_name("quaternaryContainerLight")));
@property (class, readonly) uint64_t quaternaryLight __attribute__((swift_name("quaternaryLight")));
@property (class, readonly) uint64_t quinaryContainerLight __attribute__((swift_name("quinaryContainerLight")));
@property (class, readonly) uint64_t quinaryLight __attribute__((swift_name("quinaryLight")));
@property (class, readonly) uint64_t scrimLight __attribute__((swift_name("scrimLight")));
@property (class, readonly) uint64_t secondaryContainerLight __attribute__((swift_name("secondaryContainerLight")));
@property (class, readonly) uint64_t secondaryLight __attribute__((swift_name("secondaryLight")));
@property (class, readonly) uint64_t surfaceBrightLight __attribute__((swift_name("surfaceBrightLight")));
@property (class, readonly) uint64_t surfaceContainerHighLight __attribute__((swift_name("surfaceContainerHighLight")));
@property (class, readonly) uint64_t surfaceContainerHighestLight __attribute__((swift_name("surfaceContainerHighestLight")));
@property (class, readonly) uint64_t surfaceContainerLight __attribute__((swift_name("surfaceContainerLight")));
@property (class, readonly) uint64_t surfaceContainerLowLight __attribute__((swift_name("surfaceContainerLowLight")));
@property (class, readonly) uint64_t surfaceContainerLowestLight __attribute__((swift_name("surfaceContainerLowestLight")));
@property (class, readonly) uint64_t surfaceDimLight __attribute__((swift_name("surfaceDimLight")));
@property (class, readonly) uint64_t surfaceLight __attribute__((swift_name("surfaceLight")));
@property (class, readonly) uint64_t surfaceVariantLight __attribute__((swift_name("surfaceVariantLight")));
@property (class, readonly) uint64_t tertiaryContainerLight __attribute__((swift_name("tertiaryContainerLight")));
@property (class, readonly) uint64_t tertiaryLight __attribute__((swift_name("tertiaryLight")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedExtendedThemeKt")))
@interface SharedSharedExtendedThemeKt : SharedBase
+ (SharedUi_textTextStyle *)sharedCommonBoldStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedCommonBoldStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedCommonJustifiedTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedCommonJustifiedTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedCommonTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedCommonTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedDefaultTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedDefaultTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedExtraBoldTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedExtraBoldTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedExtraLightTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedExtraLightTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedExtraPrimaryColoredBoldTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedExtraPrimaryColoredBoldTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedExtraTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedExtraTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedPrimaryColoredBoldTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedPrimaryColoredBoldTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedPrimaryColoredTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedPrimaryColoredTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedQuaternaryColoredTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedQuaternaryColoredTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedQuinaryColoredBoldTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedQuinaryColoredBoldTextStyle(fontSize:)")));
+ (SharedUi_textTextStyle *)sharedQuinaryColoredTextStyleFontSize:(int32_t)fontSize __attribute__((swift_name("sharedQuinaryColoredTextStyle(fontSize:)")));
@property (class, readonly) SharedSharedExtendedColorScheme *sharedExtendedLight __attribute__((swift_name("sharedExtendedLight")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedModuleKt")))
@interface SharedSharedModuleKt : SharedBase
@property (class, readonly) NSArray<SharedKoin_coreModule *> *sharedModule __attribute__((swift_name("sharedModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedTypography_iosKt")))
@interface SharedSharedTypography_iosKt : SharedBase
@property (class, readonly) SharedUi_textFontFamily *SharedBodyFontFamily __attribute__((swift_name("SharedBodyFontFamily")));
@property (class, readonly) SharedUi_textFontFamily *SharedDisplayFontFamily __attribute__((swift_name("SharedDisplayFontFamily")));
@property (class, readonly) SharedUi_textFontFamily *SharedExtraFontFamily __attribute__((swift_name("SharedExtraFontFamily")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedTypographyKt")))
@interface SharedSharedTypographyKt : SharedBase
@property (class, readonly) SharedMaterial3Typography *SharedTypography __attribute__((swift_name("SharedTypography")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UIModuleKt")))
@interface SharedUIModuleKt : SharedBase
@property (class, readonly) SharedKoin_coreModule *uiModule __attribute__((swift_name("uiModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WWWGlobalsKt")))
@interface SharedWWWGlobalsKt : SharedBase
+ (void)debugBuild __attribute__((swift_name("debugBuild()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResourcesResourcePlatformDetails")))
@interface SharedResourcesResourcePlatformDetails : SharedBase
- (instancetype)initWithNsBundle:(NSBundle *)nsBundle __attribute__((swift_name("init(nsBundle:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSBundle *nsBundle __attribute__((swift_name("nsBundle")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResourcesStringResource")))
@interface SharedResourcesStringResource : SharedBase
- (instancetype)initWithResourceId:(NSString *)resourceId bundle:(NSBundle *)bundle __attribute__((swift_name("init(resourceId:bundle:)"))) __attribute__((objc_designated_initializer));
- (SharedResourcesStringResource *)doCopyResourceId:(NSString *)resourceId bundle:(NSBundle *)bundle __attribute__((swift_name("doCopy(resourceId:bundle:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSBundle *bundle __attribute__((swift_name("bundle")));
@property (readonly) NSString *resourceId __attribute__((swift_name("resourceId")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlow")))
@protocol SharedKotlinx_coroutines_coreFlow
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<SharedKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreSharedFlow")))
@protocol SharedKotlinx_coroutines_coreSharedFlow <SharedKotlinx_coroutines_coreFlow>
@required
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreStateFlow")))
@protocol SharedKotlinx_coroutines_coreStateFlow <SharedKotlinx_coroutines_coreSharedFlow>
@required
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="2.1")
 *   kotlin.time.ExperimentalTime
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinInstant")))
@interface SharedKotlinInstant : SharedBase <SharedKotlinComparable>
@property (class, readonly, getter=companion) SharedKotlinInstantCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinInstant *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKotlinInstant *)minusDuration:(int64_t)duration __attribute__((swift_name("minus(duration:)")));
- (int64_t)minusOther:(SharedKotlinInstant *)other __attribute__((swift_name("minus(other:)")));
- (SharedKotlinInstant *)plusDuration:(int64_t)duration __attribute__((swift_name("plus(duration:)")));
- (int64_t)toEpochMilliseconds __attribute__((swift_name("toEpochMilliseconds()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t epochSeconds __attribute__((swift_name("epochSeconds")));
@property (readonly) int32_t nanosecondsOfSecond __attribute__((swift_name("nanosecondsOfSecond")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializationStrategy")))
@protocol SharedKotlinx_serialization_coreSerializationStrategy
@required
- (void)serializeEncoder:(id<SharedKotlinx_serialization_coreEncoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDeserializationStrategy")))
@protocol SharedKotlinx_serialization_coreDeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<SharedKotlinx_serialization_coreDecoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreKSerializer")))
@protocol SharedKotlinx_serialization_coreKSerializer <SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreDeserializationStrategy>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreKoin")))
@interface SharedKoin_coreKoin : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)close __attribute__((swift_name("close()")));
- (void)createEagerInstances __attribute__((swift_name("createEagerInstances()")));
- (SharedKoin_coreScope *)createScopeT:(id<SharedKoin_coreKoinScopeComponent>)t __attribute__((swift_name("createScope(t:)")));
- (SharedKoin_coreScope *)createScopeScopeId:(NSString *)scopeId __attribute__((swift_name("createScope(scopeId:)")));
- (SharedKoin_coreScope *)createScopeScopeId:(NSString *)scopeId source:(id _Nullable)source scopeArchetype:(SharedKoin_coreTypeQualifier * _Nullable)scopeArchetype __attribute__((swift_name("createScope(scopeId:source:scopeArchetype:)")));
- (SharedKoin_coreScope *)createScopeScopeId:(NSString *)scopeId qualifier:(id<SharedKoin_coreQualifier>)qualifier source:(id _Nullable)source scopeArchetype:(SharedKoin_coreTypeQualifier * _Nullable)scopeArchetype __attribute__((swift_name("createScope(scopeId:qualifier:source:scopeArchetype:)")));
- (void)declareInstance:(id _Nullable)instance qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier secondaryTypes:(NSArray<id<SharedKotlinKClass>> *)secondaryTypes allowOverride:(BOOL)allowOverride __attribute__((swift_name("declare(instance:qualifier:secondaryTypes:allowOverride:)")));
- (void)deletePropertyKey:(NSString *)key __attribute__((swift_name("deleteProperty(key:)")));
- (void)deleteScopeScopeId:(NSString *)scopeId __attribute__((swift_name("deleteScope(scopeId:)")));
- (id)getQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("get(qualifier:parameters:)")));
- (id _Nullable)getClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("get(clazz:qualifier:parameters:)")));
- (NSArray<id> *)getAll __attribute__((swift_name("getAll()")));
- (SharedKoin_coreScope *)getOrCreateScopeScopeId:(NSString *)scopeId __attribute__((swift_name("getOrCreateScope(scopeId:)")));
- (SharedKoin_coreScope *)getOrCreateScopeScopeId:(NSString *)scopeId qualifier:(id<SharedKoin_coreQualifier>)qualifier source:(id _Nullable)source __attribute__((swift_name("getOrCreateScope(scopeId:qualifier:source:)")));
- (id _Nullable)getOrNullQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("getOrNull(qualifier:parameters:)")));
- (id _Nullable)getOrNullClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("getOrNull(clazz:qualifier:parameters:)")));
- (id _Nullable)getPropertyKey:(NSString *)key __attribute__((swift_name("getProperty(key:)")));
- (id)getPropertyKey:(NSString *)key defaultValue:(id)defaultValue __attribute__((swift_name("getProperty(key:defaultValue:)")));
- (SharedKoin_coreScope *)getScopeScopeId:(NSString *)scopeId __attribute__((swift_name("getScope(scopeId:)")));
- (SharedKoin_coreScope * _Nullable)getScopeOrNullScopeId:(NSString *)scopeId __attribute__((swift_name("getScopeOrNull(scopeId:)")));
- (id<SharedKotlinLazy>)injectQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier mode:(SharedKotlinLazyThreadSafetyMode *)mode parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("inject(qualifier:mode:parameters:)")));
- (id<SharedKotlinLazy>)injectOrNullQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier mode:(SharedKotlinLazyThreadSafetyMode *)mode parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("injectOrNull(qualifier:mode:parameters:)")));
- (void)loadModulesModules:(NSArray<SharedKoin_coreModule *> *)modules allowOverride:(BOOL)allowOverride createEagerInstances:(BOOL)createEagerInstances __attribute__((swift_name("loadModules(modules:allowOverride:createEagerInstances:)")));
- (void)setPropertyKey:(NSString *)key value:(id)value __attribute__((swift_name("setProperty(key:value:)")));
- (void)setupLoggerLogger:(SharedKoin_coreLogger *)logger __attribute__((swift_name("setupLogger(logger:)")));
- (void)unloadModulesModules:(NSArray<SharedKoin_coreModule *> *)modules __attribute__((swift_name("unloadModules(modules:)")));
@property (readonly) SharedKoin_coreExtensionManager *extensionManager __attribute__((swift_name("extensionManager")));
@property (readonly) SharedKoin_coreInstanceRegistry *instanceRegistry __attribute__((swift_name("instanceRegistry")));
@property (readonly) SharedKoin_coreLogger *logger __attribute__((swift_name("logger")));
@property (readonly) SharedKoin_coreOptionRegistry *optionRegistry __attribute__((swift_name("optionRegistry")));
@property (readonly) SharedKoin_corePropertyRegistry *propertyRegistry __attribute__((swift_name("propertyRegistry")));
@property (readonly) SharedKoin_coreCoreResolver *resolver __attribute__((swift_name("resolver")));
@property (readonly) SharedKoin_coreScopeRegistry *scopeRegistry __attribute__((swift_name("scopeRegistry")));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface SharedKotlinRuntimeException : SharedKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface SharedKotlinIllegalStateException : SharedKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface SharedKotlinCancellationException : SharedKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedKotlinArray<T> : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("Datastore_coreDataStore")))
@protocol SharedDatastore_coreDataStore
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateDataTransform:(id<SharedKotlinSuspendFunction1>)transform completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateData(transform:completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreFlow> data __attribute__((swift_name("data")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinCoroutineContext")))
@protocol SharedKotlinCoroutineContext
@required
- (id _Nullable)foldInitial:(id _Nullable)initial operation:(id _Nullable (^)(id _Nullable, id<SharedKotlinCoroutineContextElement>))operation __attribute__((swift_name("fold(initial:operation:)")));
- (id<SharedKotlinCoroutineContextElement> _Nullable)getKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("get(key:)")));
- (id<SharedKotlinCoroutineContext>)minusKeyKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("minusKey(key:)")));
- (id<SharedKotlinCoroutineContext>)plusContext:(id<SharedKotlinCoroutineContext>)context __attribute__((swift_name("plus(context:)")));
@end

__attribute__((swift_name("KotlinCoroutineContextElement")))
@protocol SharedKotlinCoroutineContextElement <SharedKotlinCoroutineContext>
@required
@property (readonly) id<SharedKotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinAbstractCoroutineContextElement")))
@interface SharedKotlinAbstractCoroutineContextElement : SharedBase <SharedKotlinCoroutineContextElement>
- (instancetype)initWithKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id<SharedKotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinContinuationInterceptor")))
@protocol SharedKotlinContinuationInterceptor <SharedKotlinCoroutineContextElement>
@required
- (id<SharedKotlinContinuation>)interceptContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (void)releaseInterceptedContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineDispatcher")))
@interface SharedKotlinx_coroutines_coreCoroutineDispatcher : SharedKotlinAbstractCoroutineContextElement <SharedKotlinContinuationInterceptor>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKotlinx_coroutines_coreCoroutineDispatcherKey *companion __attribute__((swift_name("companion")));
- (void)dispatchContext:(id<SharedKotlinCoroutineContext>)context block:(id<SharedKotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatch(context:block:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (void)dispatchYieldContext:(id<SharedKotlinCoroutineContext>)context block:(id<SharedKotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatchYield(context:block:)")));
- (id<SharedKotlinContinuation>)interceptContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (BOOL)isDispatchNeededContext:(id<SharedKotlinCoroutineContext>)context __attribute__((swift_name("isDispatchNeeded(context:)")));
- (SharedKotlinx_coroutines_coreCoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism name:(NSString * _Nullable)name __attribute__((swift_name("limitedParallelism(parallelism:name:)")));
- (SharedKotlinx_coroutines_coreCoroutineDispatcher *)plusOther:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)other __attribute__((swift_name("plus(other:)"))) __attribute__((unavailable("Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")));
- (void)releaseInterceptedContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedKotlinEnumCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/TimeZoneSerializer))
*/
__attribute__((swift_name("Kotlinx_datetimeTimeZone")))
@interface SharedKotlinx_datetimeTimeZone : SharedBase
@property (class, readonly, getter=companion) SharedKotlinx_datetimeTimeZoneCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKotlinInstant *)toInstant:(SharedKotlinx_datetimeLocalDateTime *)receiver youShallNotPass:(SharedKotlinx_datetimeOverloadMarker *)youShallNotPass __attribute__((swift_name("toInstant(_:youShallNotPass:)")));
- (SharedKotlinx_datetimeLocalDateTime *)toLocalDateTime:(SharedKotlinInstant *)receiver __attribute__((swift_name("toLocalDateTime(_:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((swift_name("LibraryResource")))
@interface SharedLibraryResource : SharedBase
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryDrawableResource")))
@interface SharedLibraryDrawableResource : SharedLibraryResource
- (instancetype)initWithId:(NSString *)id items:(NSSet<SharedLibraryResourceItem *> *)items __attribute__((swift_name("init(id:items:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinPair")))
@interface SharedKotlinPair<__covariant A, __covariant B> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("init(first:second:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinPair<A, B> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("doCopy(first:second:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreJob")))
@protocol SharedKotlinx_coroutines_coreJob <SharedKotlinCoroutineContextElement>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (id<SharedKotlinx_coroutines_coreChildHandle>)attachChildChild:(id<SharedKotlinx_coroutines_coreChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (void)cancelCause:(SharedKotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (SharedKotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<SharedKotlinx_coroutines_coreDisposableHandle>)invokeOnCompletionHandler:(void (^)(SharedKotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (id<SharedKotlinx_coroutines_coreDisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(SharedKotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreJob>)plusOther_:(id<SharedKotlinx_coroutines_coreJob>)other __attribute__((swift_name("plus(other_:)"))) __attribute__((unavailable("Operator '+' on two Job objects is meaningless. Job is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The job to the right of `+` just replaces the job the left of `+`.")));
- (BOOL)start __attribute__((swift_name("start()")));
@property (readonly) id<SharedKotlinSequence> children __attribute__((swift_name("children")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property (readonly) id<SharedKotlinx_coroutines_coreSelectClause0> onJoin __attribute__((swift_name("onJoin")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@property (readonly) id<SharedKotlinx_coroutines_coreJob> _Nullable parent __attribute__((swift_name("parent")));
@end

__attribute__((swift_name("KotlinFunction")))
@protocol SharedKotlinFunction
@required
@end

__attribute__((swift_name("KotlinSuspendFunction1")))
@protocol SharedKotlinSuspendFunction1 <SharedKotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:completionHandler:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/serialization/json/JsonElementSerializer))
*/
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement")))
@interface SharedKotlinx_serialization_jsonJsonElement : SharedBase
@property (class, readonly, getter=companion) SharedKotlinx_serialization_jsonJsonElementCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((swift_name("KotlinMutableIterator")))
@protocol SharedKotlinMutableIterator <SharedKotlinIterator>
@required
- (void)remove __attribute__((swift_name("remove()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinByteArray")))
@interface SharedKotlinByteArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedByte *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int8_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinByteIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int8_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUnit")))
@interface SharedKotlinUnit : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinDoubleArray")))
@interface SharedKotlinDoubleArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedDouble *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (double)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinDoubleIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(double)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlowCollector")))
@protocol SharedKotlinx_coroutines_coreFlowCollector
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreMutableSharedFlow")))
@protocol SharedKotlinx_coroutines_coreMutableSharedFlow <SharedKotlinx_coroutines_coreSharedFlow, SharedKotlinx_coroutines_coreFlowCollector>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(id _Nullable)value __attribute__((swift_name("tryEmit(value:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreMutableStateFlow")))
@protocol SharedKotlinx_coroutines_coreMutableStateFlow <SharedKotlinx_coroutines_coreStateFlow, SharedKotlinx_coroutines_coreMutableSharedFlow>
@required
- (void)setValue:(id _Nullable)value __attribute__((swift_name("setValue(_:)")));
- (BOOL)compareAndSetExpect:(id _Nullable)expect update:(id _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryFontResource")))
@interface SharedLibraryFontResource : SharedLibraryResource
- (instancetype)initWithId:(NSString *)id items:(NSSet<SharedLibraryResourceItem *> *)items __attribute__((swift_name("init(id:items:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryPluralStringResource")))
@interface SharedLibraryPluralStringResource : SharedLibraryResource
- (instancetype)initWithId:(NSString *)id key:(NSString *)key items:(NSSet<SharedLibraryResourceItem *> *)items __attribute__((swift_name("init(id:key:items:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSString *key __attribute__((swift_name("key")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryStringArrayResource")))
@interface SharedLibraryStringArrayResource : SharedLibraryResource
- (instancetype)initWithId:(NSString *)id key:(NSString *)key items:(NSSet<SharedLibraryResourceItem *> *)items __attribute__((swift_name("init(id:key:items:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSString *key __attribute__((swift_name("key")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryStringResource")))
@interface SharedLibraryStringResource : SharedLibraryResource
- (instancetype)initWithId:(NSString *)id key:(NSString *)key items:(NSSet<SharedLibraryResourceItem *> *)items __attribute__((swift_name("init(id:key:items:)"))) __attribute__((objc_designated_initializer));
@property (readonly) NSString *key __attribute__((swift_name("key")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreModule")))
@interface SharedKoin_coreModule : SharedBase
- (instancetype)initWith_createdAtStart:(BOOL)_createdAtStart __attribute__((swift_name("init(_createdAtStart:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedKoin_coreKoinDefinition<id> *)factoryQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier definition:(id _Nullable (^)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *))definition __attribute__((swift_name("factory(qualifier:definition:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (void)includesModule:(SharedKotlinArray<SharedKoin_coreModule *> *)module __attribute__((swift_name("includes(module:)")));
- (void)includesModule_:(id)module __attribute__((swift_name("includes(module_:)")));
- (void)indexPrimaryTypeInstanceFactory:(SharedKoin_coreInstanceFactory<id> *)instanceFactory __attribute__((swift_name("indexPrimaryType(instanceFactory:)")));
- (void)indexSecondaryTypesInstanceFactory:(SharedKoin_coreInstanceFactory<id> *)instanceFactory __attribute__((swift_name("indexSecondaryTypes(instanceFactory:)")));
- (NSArray<SharedKoin_coreModule *> *)plusModules:(NSArray<SharedKoin_coreModule *> *)modules __attribute__((swift_name("plus(modules:)")));
- (NSArray<SharedKoin_coreModule *> *)plusModule:(SharedKoin_coreModule *)module __attribute__((swift_name("plus(module:)")));
- (void)prepareForCreationAtStartInstanceFactory:(SharedKoin_coreSingleInstanceFactory<id> *)instanceFactory __attribute__((swift_name("prepareForCreationAtStart(instanceFactory:)")));
- (void)scopeScopeSet:(void (^)(SharedKoin_coreScopeDSL *))scopeSet __attribute__((swift_name("scope(scopeSet:)")));
- (void)scopeQualifier:(id<SharedKoin_coreQualifier>)qualifier scopeSet:(void (^)(SharedKoin_coreScopeDSL *))scopeSet __attribute__((swift_name("scope(qualifier:scopeSet:)")));
- (SharedKoin_coreKoinDefinition<id> *)singleQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier createdAtStart:(BOOL)createdAtStart definition:(id _Nullable (^)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *))definition __attribute__((swift_name("single(qualifier:createdAtStart:definition:)")));
@property (readonly) SharedMutableSet<SharedKoin_coreSingleInstanceFactory<id> *> *eagerInstances __attribute__((swift_name("eagerInstances")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSMutableArray<SharedKoin_coreModule *> *includedModules __attribute__((swift_name("includedModules")));
@property (readonly) BOOL isLoaded __attribute__((swift_name("isLoaded")));
@property (readonly) SharedMutableDictionary<NSString *, SharedKoin_coreInstanceFactory<id> *> *mappings __attribute__((swift_name("mappings")));
@property (readonly) SharedMutableSet<id<SharedKoin_coreQualifier>> *scopes __attribute__((swift_name("scopes")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Material3ColorScheme")))
@interface SharedMaterial3ColorScheme : SharedBase
- (instancetype)initWithPrimary:(uint64_t)primary onPrimary:(uint64_t)onPrimary primaryContainer:(uint64_t)primaryContainer onPrimaryContainer:(uint64_t)onPrimaryContainer inversePrimary:(uint64_t)inversePrimary secondary:(uint64_t)secondary onSecondary:(uint64_t)onSecondary secondaryContainer:(uint64_t)secondaryContainer onSecondaryContainer:(uint64_t)onSecondaryContainer tertiary:(uint64_t)tertiary onTertiary:(uint64_t)onTertiary tertiaryContainer:(uint64_t)tertiaryContainer onTertiaryContainer:(uint64_t)onTertiaryContainer background:(uint64_t)background onBackground:(uint64_t)onBackground surface:(uint64_t)surface onSurface:(uint64_t)onSurface surfaceVariant:(uint64_t)surfaceVariant onSurfaceVariant:(uint64_t)onSurfaceVariant surfaceTint:(uint64_t)surfaceTint inverseSurface:(uint64_t)inverseSurface inverseOnSurface:(uint64_t)inverseOnSurface error:(uint64_t)error onError:(uint64_t)onError errorContainer:(uint64_t)errorContainer onErrorContainer:(uint64_t)onErrorContainer outline:(uint64_t)outline outlineVariant:(uint64_t)outlineVariant scrim:(uint64_t)scrim __attribute__((swift_name("init(primary:onPrimary:primaryContainer:onPrimaryContainer:inversePrimary:secondary:onSecondary:secondaryContainer:onSecondaryContainer:tertiary:onTertiary:tertiaryContainer:onTertiaryContainer:background:onBackground:surface:onSurface:surfaceVariant:onSurfaceVariant:surfaceTint:inverseSurface:inverseOnSurface:error:onError:errorContainer:onErrorContainer:outline:outlineVariant:scrim:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("Use constructor with additional 'surfaceContainer' roles.")));
- (instancetype)initWithPrimary:(uint64_t)primary onPrimary:(uint64_t)onPrimary primaryContainer:(uint64_t)primaryContainer onPrimaryContainer:(uint64_t)onPrimaryContainer inversePrimary:(uint64_t)inversePrimary secondary:(uint64_t)secondary onSecondary:(uint64_t)onSecondary secondaryContainer:(uint64_t)secondaryContainer onSecondaryContainer:(uint64_t)onSecondaryContainer tertiary:(uint64_t)tertiary onTertiary:(uint64_t)onTertiary tertiaryContainer:(uint64_t)tertiaryContainer onTertiaryContainer:(uint64_t)onTertiaryContainer background:(uint64_t)background onBackground:(uint64_t)onBackground surface:(uint64_t)surface onSurface:(uint64_t)onSurface surfaceVariant:(uint64_t)surfaceVariant onSurfaceVariant:(uint64_t)onSurfaceVariant surfaceTint:(uint64_t)surfaceTint inverseSurface:(uint64_t)inverseSurface inverseOnSurface:(uint64_t)inverseOnSurface error:(uint64_t)error onError:(uint64_t)onError errorContainer:(uint64_t)errorContainer onErrorContainer:(uint64_t)onErrorContainer outline:(uint64_t)outline outlineVariant:(uint64_t)outlineVariant scrim:(uint64_t)scrim surfaceBright:(uint64_t)surfaceBright surfaceDim:(uint64_t)surfaceDim surfaceContainer:(uint64_t)surfaceContainer surfaceContainerHigh:(uint64_t)surfaceContainerHigh surfaceContainerHighest:(uint64_t)surfaceContainerHighest surfaceContainerLow:(uint64_t)surfaceContainerLow surfaceContainerLowest:(uint64_t)surfaceContainerLowest __attribute__((swift_name("init(primary:onPrimary:primaryContainer:onPrimaryContainer:inversePrimary:secondary:onSecondary:secondaryContainer:onSecondaryContainer:tertiary:onTertiary:tertiaryContainer:onTertiaryContainer:background:onBackground:surface:onSurface:surfaceVariant:onSurfaceVariant:surfaceTint:inverseSurface:inverseOnSurface:error:onError:errorContainer:onErrorContainer:outline:outlineVariant:scrim:surfaceBright:surfaceDim:surfaceContainer:surfaceContainerHigh:surfaceContainerHighest:surfaceContainerLow:surfaceContainerLowest:)"))) __attribute__((objc_designated_initializer));
- (SharedMaterial3ColorScheme *)doCopyPrimary:(uint64_t)primary onPrimary:(uint64_t)onPrimary primaryContainer:(uint64_t)primaryContainer onPrimaryContainer:(uint64_t)onPrimaryContainer inversePrimary:(uint64_t)inversePrimary secondary:(uint64_t)secondary onSecondary:(uint64_t)onSecondary secondaryContainer:(uint64_t)secondaryContainer onSecondaryContainer:(uint64_t)onSecondaryContainer tertiary:(uint64_t)tertiary onTertiary:(uint64_t)onTertiary tertiaryContainer:(uint64_t)tertiaryContainer onTertiaryContainer:(uint64_t)onTertiaryContainer background:(uint64_t)background onBackground:(uint64_t)onBackground surface:(uint64_t)surface onSurface:(uint64_t)onSurface surfaceVariant:(uint64_t)surfaceVariant onSurfaceVariant:(uint64_t)onSurfaceVariant surfaceTint:(uint64_t)surfaceTint inverseSurface:(uint64_t)inverseSurface inverseOnSurface:(uint64_t)inverseOnSurface error:(uint64_t)error onError:(uint64_t)onError errorContainer:(uint64_t)errorContainer onErrorContainer:(uint64_t)onErrorContainer outline:(uint64_t)outline outlineVariant:(uint64_t)outlineVariant scrim:(uint64_t)scrim surfaceBright:(uint64_t)surfaceBright surfaceDim:(uint64_t)surfaceDim surfaceContainer:(uint64_t)surfaceContainer surfaceContainerHigh:(uint64_t)surfaceContainerHigh surfaceContainerHighest:(uint64_t)surfaceContainerHighest surfaceContainerLow:(uint64_t)surfaceContainerLow surfaceContainerLowest:(uint64_t)surfaceContainerLowest __attribute__((swift_name("doCopy(primary:onPrimary:primaryContainer:onPrimaryContainer:inversePrimary:secondary:onSecondary:secondaryContainer:onSecondaryContainer:tertiary:onTertiary:tertiaryContainer:onTertiaryContainer:background:onBackground:surface:onSurface:surfaceVariant:onSurfaceVariant:surfaceTint:inverseSurface:inverseOnSurface:error:onError:errorContainer:onErrorContainer:outline:outlineVariant:scrim:surfaceBright:surfaceDim:surfaceContainer:surfaceContainerHigh:surfaceContainerHighest:surfaceContainerLow:surfaceContainerLowest:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) uint64_t background __attribute__((swift_name("background")));
@property (readonly) uint64_t error __attribute__((swift_name("error")));
@property (readonly) uint64_t errorContainer __attribute__((swift_name("errorContainer")));
@property (readonly) uint64_t inverseOnSurface __attribute__((swift_name("inverseOnSurface")));
@property (readonly) uint64_t inversePrimary __attribute__((swift_name("inversePrimary")));
@property (readonly) uint64_t inverseSurface __attribute__((swift_name("inverseSurface")));
@property (readonly) uint64_t onBackground __attribute__((swift_name("onBackground")));
@property (readonly) uint64_t onError __attribute__((swift_name("onError")));
@property (readonly) uint64_t onErrorContainer __attribute__((swift_name("onErrorContainer")));
@property (readonly) uint64_t onPrimary __attribute__((swift_name("onPrimary")));
@property (readonly) uint64_t onPrimaryContainer __attribute__((swift_name("onPrimaryContainer")));
@property (readonly) uint64_t onSecondary __attribute__((swift_name("onSecondary")));
@property (readonly) uint64_t onSecondaryContainer __attribute__((swift_name("onSecondaryContainer")));
@property (readonly) uint64_t onSurface __attribute__((swift_name("onSurface")));
@property (readonly) uint64_t onSurfaceVariant __attribute__((swift_name("onSurfaceVariant")));
@property (readonly) uint64_t onTertiary __attribute__((swift_name("onTertiary")));
@property (readonly) uint64_t onTertiaryContainer __attribute__((swift_name("onTertiaryContainer")));
@property (readonly) uint64_t outline __attribute__((swift_name("outline")));
@property (readonly) uint64_t outlineVariant __attribute__((swift_name("outlineVariant")));
@property (readonly) uint64_t primary __attribute__((swift_name("primary")));
@property (readonly) uint64_t primaryContainer __attribute__((swift_name("primaryContainer")));
@property (readonly) uint64_t scrim __attribute__((swift_name("scrim")));
@property (readonly) uint64_t secondary __attribute__((swift_name("secondary")));
@property (readonly) uint64_t secondaryContainer __attribute__((swift_name("secondaryContainer")));
@property (readonly) uint64_t surface __attribute__((swift_name("surface")));
@property (readonly) uint64_t surfaceBright __attribute__((swift_name("surfaceBright")));
@property (readonly) uint64_t surfaceContainer __attribute__((swift_name("surfaceContainer")));
@property (readonly) uint64_t surfaceContainerHigh __attribute__((swift_name("surfaceContainerHigh")));
@property (readonly) uint64_t surfaceContainerHighest __attribute__((swift_name("surfaceContainerHighest")));
@property (readonly) uint64_t surfaceContainerLow __attribute__((swift_name("surfaceContainerLow")));
@property (readonly) uint64_t surfaceContainerLowest __attribute__((swift_name("surfaceContainerLowest")));
@property (readonly) uint64_t surfaceDim __attribute__((swift_name("surfaceDim")));
@property (readonly) uint64_t surfaceTint __attribute__((swift_name("surfaceTint")));
@property (readonly) uint64_t surfaceVariant __attribute__((swift_name("surfaceVariant")));
@property (readonly) uint64_t tertiary __attribute__((swift_name("tertiary")));
@property (readonly) uint64_t tertiaryContainer __attribute__((swift_name("tertiaryContainer")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextStyle")))
@interface SharedUi_textTextStyle : SharedBase
- (instancetype)initWithColor:(uint64_t)color fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle textAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformTextStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("init(color:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:drawStyle:textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithBrush:(SharedUi_graphicsBrush * _Nullable)brush alpha:(float)alpha fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle textAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformTextStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("init(brush:alpha:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:drawStyle:textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textTextStyleCompanion *companion __attribute__((swift_name("companion")));
- (SharedUi_textTextStyle *)doCopyColor:(uint64_t)color fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle textAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformTextStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("doCopy(color:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:drawStyle:textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)")));
- (SharedUi_textTextStyle *)doCopyBrush:(SharedUi_graphicsBrush * _Nullable)brush alpha:(float)alpha fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle textAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformTextStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("doCopy(brush:alpha:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:drawStyle:textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (BOOL)hasSameDrawAffectingAttributesOther:(SharedUi_textTextStyle *)other __attribute__((swift_name("hasSameDrawAffectingAttributes(other:)")));
- (BOOL)hasSameLayoutAffectingAttributesOther:(SharedUi_textTextStyle *)other __attribute__((swift_name("hasSameLayoutAffectingAttributes(other:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)mergeOther:(SharedUi_textParagraphStyle *)other __attribute__((swift_name("merge(other:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)mergeOther_:(SharedUi_textSpanStyle *)other __attribute__((swift_name("merge(other_:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)mergeOther__:(SharedUi_textTextStyle * _Nullable)other __attribute__((swift_name("merge(other__:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)mergeColor:(uint64_t)color fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle textAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens platformStyle:(SharedUi_textPlatformTextStyle * _Nullable)platformStyle textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("merge(color:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:drawStyle:textAlign:textDirection:lineHeight:textIndent:lineHeightStyle:lineBreak:hyphens:platformStyle:textMotion:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)plusOther:(SharedUi_textParagraphStyle *)other __attribute__((swift_name("plus(other:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)plusOther_:(SharedUi_textSpanStyle *)other __attribute__((swift_name("plus(other_:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textTextStyle *)plusOther__:(SharedUi_textTextStyle *)other __attribute__((swift_name("plus(other__:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textParagraphStyle *)toParagraphStyle __attribute__((swift_name("toParagraphStyle()")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textSpanStyle *)toSpanStyle __attribute__((swift_name("toSpanStyle()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float alpha __attribute__((swift_name("alpha")));
@property (readonly) uint64_t background __attribute__((swift_name("background")));
@property (readonly) id _Nullable baselineShift __attribute__((swift_name("baselineShift")));
@property (readonly) SharedUi_graphicsBrush * _Nullable brush __attribute__((swift_name("brush")));
@property (readonly) uint64_t color __attribute__((swift_name("color")));
@property (readonly) id _Nullable deprecated_boxing_hyphens __attribute__((swift_name("deprecated_boxing_hyphens"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_lineBreak __attribute__((swift_name("deprecated_boxing_lineBreak"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_textAlign __attribute__((swift_name("deprecated_boxing_textAlign"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_textDirection __attribute__((swift_name("deprecated_boxing_textDirection"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) SharedUi_graphicsDrawStyle * _Nullable drawStyle __attribute__((swift_name("drawStyle")));
@property (readonly) SharedUi_textFontFamily * _Nullable fontFamily __attribute__((swift_name("fontFamily")));
@property (readonly) NSString * _Nullable fontFeatureSettings __attribute__((swift_name("fontFeatureSettings")));
@property (readonly) int64_t fontSize __attribute__((swift_name("fontSize")));
@property (readonly) id _Nullable fontStyle __attribute__((swift_name("fontStyle")));
@property (readonly) id _Nullable fontSynthesis __attribute__((swift_name("fontSynthesis")));
@property (readonly) SharedUi_textFontWeight * _Nullable fontWeight __attribute__((swift_name("fontWeight")));
@property (readonly) int32_t hyphens __attribute__((swift_name("hyphens")));
@property (readonly) int64_t letterSpacing __attribute__((swift_name("letterSpacing")));
@property (readonly) int32_t lineBreak __attribute__((swift_name("lineBreak")));
@property (readonly) int64_t lineHeight __attribute__((swift_name("lineHeight")));
@property (readonly) SharedUi_textLineHeightStyle * _Nullable lineHeightStyle __attribute__((swift_name("lineHeightStyle")));
@property (readonly) SharedUi_textLocaleList * _Nullable localeList __attribute__((swift_name("localeList")));
@property (readonly) SharedUi_textPlatformTextStyle * _Nullable platformStyle __attribute__((swift_name("platformStyle")));
@property (readonly) SharedUi_graphicsShadow * _Nullable shadow __attribute__((swift_name("shadow")));
@property (readonly) int32_t textAlign __attribute__((swift_name("textAlign")));
@property (readonly) SharedUi_textTextDecoration * _Nullable textDecoration __attribute__((swift_name("textDecoration")));
@property (readonly) int32_t textDirection __attribute__((swift_name("textDirection")));
@property (readonly) SharedUi_textTextGeometricTransform * _Nullable textGeometricTransform __attribute__((swift_name("textGeometricTransform")));
@property (readonly) SharedUi_textTextIndent * _Nullable textIndent __attribute__((swift_name("textIndent")));
@property (readonly) SharedUi_textTextMotion * _Nullable textMotion __attribute__((swift_name("textMotion")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((swift_name("Ui_textFontFamily")))
@interface SharedUi_textFontFamily : SharedBase
@property (class, readonly, getter=companion) SharedUi_textFontFamilyCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL canLoadSynchronously __attribute__((swift_name("canLoadSynchronously")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Material3Typography")))
@interface SharedMaterial3Typography : SharedBase
- (instancetype)initWithDisplayLarge:(SharedUi_textTextStyle *)displayLarge displayMedium:(SharedUi_textTextStyle *)displayMedium displaySmall:(SharedUi_textTextStyle *)displaySmall headlineLarge:(SharedUi_textTextStyle *)headlineLarge headlineMedium:(SharedUi_textTextStyle *)headlineMedium headlineSmall:(SharedUi_textTextStyle *)headlineSmall titleLarge:(SharedUi_textTextStyle *)titleLarge titleMedium:(SharedUi_textTextStyle *)titleMedium titleSmall:(SharedUi_textTextStyle *)titleSmall bodyLarge:(SharedUi_textTextStyle *)bodyLarge bodyMedium:(SharedUi_textTextStyle *)bodyMedium bodySmall:(SharedUi_textTextStyle *)bodySmall labelLarge:(SharedUi_textTextStyle *)labelLarge labelMedium:(SharedUi_textTextStyle *)labelMedium labelSmall:(SharedUi_textTextStyle *)labelSmall __attribute__((swift_name("init(displayLarge:displayMedium:displaySmall:headlineLarge:headlineMedium:headlineSmall:titleLarge:titleMedium:titleSmall:bodyLarge:bodyMedium:bodySmall:labelLarge:labelMedium:labelSmall:)"))) __attribute__((objc_designated_initializer));
- (SharedMaterial3Typography *)doCopyDisplayLarge:(SharedUi_textTextStyle *)displayLarge displayMedium:(SharedUi_textTextStyle *)displayMedium displaySmall:(SharedUi_textTextStyle *)displaySmall headlineLarge:(SharedUi_textTextStyle *)headlineLarge headlineMedium:(SharedUi_textTextStyle *)headlineMedium headlineSmall:(SharedUi_textTextStyle *)headlineSmall titleLarge:(SharedUi_textTextStyle *)titleLarge titleMedium:(SharedUi_textTextStyle *)titleMedium titleSmall:(SharedUi_textTextStyle *)titleSmall bodyLarge:(SharedUi_textTextStyle *)bodyLarge bodyMedium:(SharedUi_textTextStyle *)bodyMedium bodySmall:(SharedUi_textTextStyle *)bodySmall labelLarge:(SharedUi_textTextStyle *)labelLarge labelMedium:(SharedUi_textTextStyle *)labelMedium labelSmall:(SharedUi_textTextStyle *)labelSmall __attribute__((swift_name("doCopy(displayLarge:displayMedium:displaySmall:headlineLarge:headlineMedium:headlineSmall:titleLarge:titleMedium:titleSmall:bodyLarge:bodyMedium:bodySmall:labelLarge:labelMedium:labelSmall:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedUi_textTextStyle *bodyLarge __attribute__((swift_name("bodyLarge")));
@property (readonly) SharedUi_textTextStyle *bodyMedium __attribute__((swift_name("bodyMedium")));
@property (readonly) SharedUi_textTextStyle *bodySmall __attribute__((swift_name("bodySmall")));
@property (readonly) SharedUi_textTextStyle *displayLarge __attribute__((swift_name("displayLarge")));
@property (readonly) SharedUi_textTextStyle *displayMedium __attribute__((swift_name("displayMedium")));
@property (readonly) SharedUi_textTextStyle *displaySmall __attribute__((swift_name("displaySmall")));
@property (readonly) SharedUi_textTextStyle *headlineLarge __attribute__((swift_name("headlineLarge")));
@property (readonly) SharedUi_textTextStyle *headlineMedium __attribute__((swift_name("headlineMedium")));
@property (readonly) SharedUi_textTextStyle *headlineSmall __attribute__((swift_name("headlineSmall")));
@property (readonly) SharedUi_textTextStyle *labelLarge __attribute__((swift_name("labelLarge")));
@property (readonly) SharedUi_textTextStyle *labelMedium __attribute__((swift_name("labelMedium")));
@property (readonly) SharedUi_textTextStyle *labelSmall __attribute__((swift_name("labelSmall")));
@property (readonly) SharedUi_textTextStyle *titleLarge __attribute__((swift_name("titleLarge")));
@property (readonly) SharedUi_textTextStyle *titleMedium __attribute__((swift_name("titleMedium")));
@property (readonly) SharedUi_textTextStyle *titleSmall __attribute__((swift_name("titleSmall")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinInstant.Companion")))
@interface SharedKotlinInstantCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinInstantCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinInstant *)fromEpochMillisecondsEpochMilliseconds:(int64_t)epochMilliseconds __attribute__((swift_name("fromEpochMilliseconds(epochMilliseconds:)")));
- (SharedKotlinInstant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment:(int32_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment:)")));
- (SharedKotlinInstant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment_:(int64_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment_:)")));
- (SharedKotlinInstant *)now __attribute__((swift_name("now()"))) __attribute__((unavailable("Use Clock.System.now() instead")));
- (SharedKotlinInstant *)parseInput:(id)input __attribute__((swift_name("parse(input:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="2.2")
*/
- (SharedKotlinInstant * _Nullable)parseOrNullInput:(id)input __attribute__((swift_name("parseOrNull(input:)")));
@property (readonly) SharedKotlinInstant *DISTANT_FUTURE __attribute__((swift_name("DISTANT_FUTURE")));
@property (readonly) SharedKotlinInstant *DISTANT_PAST __attribute__((swift_name("DISTANT_PAST")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreEncoder")))
@protocol SharedKotlinx_serialization_coreEncoder
@required
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginCollectionDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialDescriptor")))
@protocol SharedKotlinx_serialization_coreSerialDescriptor
@required
- (NSArray<id<SharedKotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));
- (id<SharedKotlinx_serialization_coreSerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));
@property (readonly) NSArray<id<SharedKotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));
@property (readonly) SharedKotlinx_serialization_coreSerialKind *kind __attribute__((swift_name("kind")));
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDecoder")))
@protocol SharedKotlinx_serialization_coreDecoder
@required
- (id<SharedKotlinx_serialization_coreCompositeDecoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (SharedKotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Koin_coreLockable")))
@interface SharedKoin_coreLockable : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreScope")))
@interface SharedKoin_coreScope : SharedKoin_coreLockable
- (instancetype)initWithScopeQualifier:(id<SharedKoin_coreQualifier>)scopeQualifier id:(NSString *)id isRoot:(BOOL)isRoot scopeArchetype:(SharedKoin_coreTypeQualifier * _Nullable)scopeArchetype _koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(scopeQualifier:id:isRoot:scopeArchetype:_koin:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (void)close __attribute__((swift_name("close()")));
- (void)declareInstance:(id _Nullable)instance qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier secondaryTypes:(NSArray<id<SharedKotlinKClass>> *)secondaryTypes allowOverride:(BOOL)allowOverride holdInstance:(BOOL)holdInstance __attribute__((swift_name("declare(instance:qualifier:secondaryTypes:allowOverride:holdInstance:)")));
- (id)getQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("get(qualifier:parameters:)")));
- (id _Nullable)getClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("get(clazz:qualifier:parameters:)")));
- (NSArray<id> *)getAll __attribute__((swift_name("getAll()")));
- (NSArray<id> *)getAllClazz:(id<SharedKotlinKClass>)clazz __attribute__((swift_name("getAll(clazz:)")));
- (SharedKoin_coreKoin *)getKoin __attribute__((swift_name("getKoin()")));
- (id _Nullable)getOrNullQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("getOrNull(qualifier:parameters:)")));
- (id _Nullable)getOrNullClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("getOrNull(clazz:qualifier:parameters:)")));
- (id)getPropertyKey:(NSString *)key __attribute__((swift_name("getProperty(key:)")));
- (id)getPropertyKey:(NSString *)key defaultValue:(id)defaultValue __attribute__((swift_name("getProperty(key:defaultValue:)")));
- (id _Nullable)getPropertyOrNullKey:(NSString *)key __attribute__((swift_name("getPropertyOrNull(key:)")));
- (SharedKoin_coreScope *)getScopeScopeID:(NSString *)scopeID __attribute__((swift_name("getScope(scopeID:)")));
- (id _Nullable)getSource __attribute__((swift_name("getSource()")));
- (id _Nullable)getWithParametersClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder * _Nullable)parameters __attribute__((swift_name("getWithParameters(clazz:qualifier:parameters:)")));
- (id<SharedKotlinLazy>)injectQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier mode:(SharedKotlinLazyThreadSafetyMode *)mode parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("inject(qualifier:mode:parameters:)")));
- (id<SharedKotlinLazy>)injectOrNullQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier mode:(SharedKotlinLazyThreadSafetyMode *)mode parameters:(SharedKoin_coreParametersHolder *(^ _Nullable)(void))parameters __attribute__((swift_name("injectOrNull(qualifier:mode:parameters:)")));
- (BOOL)isNotClosed __attribute__((swift_name("isNotClosed()")));
- (void)linkToScopes:(SharedKotlinArray<SharedKoin_coreScope *> *)scopes __attribute__((swift_name("linkTo(scopes:)")));
- (void)registerCallbackCallback:(id<SharedKoin_coreScopeCallback>)callback __attribute__((swift_name("registerCallback(callback:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (void)unlinkScopes:(SharedKotlinArray<SharedKoin_coreScope *> *)scopes __attribute__((swift_name("unlink(scopes:)")));
@property (readonly) BOOL closed __attribute__((swift_name("closed")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) BOOL isRoot __attribute__((swift_name("isRoot")));
@property (readonly) SharedKoin_coreLogger *logger __attribute__((swift_name("logger")));
@property (readonly) SharedKoin_coreTypeQualifier * _Nullable scopeArchetype __attribute__((swift_name("scopeArchetype")));
@property (readonly) id<SharedKoin_coreQualifier> scopeQualifier __attribute__((swift_name("scopeQualifier")));
@property id _Nullable sourceValue __attribute__((swift_name("sourceValue")));
@end

__attribute__((swift_name("Koin_coreKoinScopeComponent")))
@protocol SharedKoin_coreKoinScopeComponent <SharedKoin_coreKoinComponent>
@required
@property (readonly) SharedKoin_coreScope *scope __attribute__((swift_name("scope")));
@end

__attribute__((swift_name("Koin_coreQualifier")))
@protocol SharedKoin_coreQualifier
@required
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreTypeQualifier")))
@interface SharedKoin_coreTypeQualifier : SharedBase <SharedKoin_coreQualifier>
- (instancetype)initWithType:(id<SharedKotlinKClass>)type __attribute__((swift_name("init(type:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<SharedKotlinKClass> type __attribute__((swift_name("type")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((swift_name("KotlinKDeclarationContainer")))
@protocol SharedKotlinKDeclarationContainer
@required
@end

__attribute__((swift_name("KotlinKAnnotatedElement")))
@protocol SharedKotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((swift_name("KotlinKClassifier")))
@protocol SharedKotlinKClassifier
@required
@end

__attribute__((swift_name("KotlinKClass")))
@protocol SharedKotlinKClass <SharedKotlinKDeclarationContainer, SharedKotlinKAnnotatedElement, SharedKotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

__attribute__((swift_name("Koin_coreParametersHolder")))
@interface SharedKoin_coreParametersHolder : SharedBase
- (instancetype)initWith_values:(NSMutableArray<id> *)_values useIndexedValues:(SharedBoolean * _Nullable)useIndexedValues __attribute__((swift_name("init(_values:useIndexedValues:)"))) __attribute__((objc_designated_initializer));
- (SharedKoin_coreParametersHolder *)addValue:(id)value __attribute__((swift_name("add(value:)")));
- (id _Nullable)component1 __attribute__((swift_name("component1()")));
- (id _Nullable)component2 __attribute__((swift_name("component2()")));
- (id _Nullable)component3 __attribute__((swift_name("component3()")));
- (id _Nullable)component4 __attribute__((swift_name("component4()")));
- (id _Nullable)component5 __attribute__((swift_name("component5()")));
- (id _Nullable)elementAtI:(int32_t)i clazz:(id<SharedKotlinKClass>)clazz __attribute__((swift_name("elementAt(i:clazz:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (id)get __attribute__((swift_name("get()")));
- (id _Nullable)getI:(int32_t)i __attribute__((swift_name("get(i:)")));
- (id _Nullable)getOrNull __attribute__((swift_name("getOrNull()")));
- (id _Nullable)getOrNullClazz:(id<SharedKotlinKClass>)clazz __attribute__((swift_name("getOrNull(clazz:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKoin_coreParametersHolder *)insertIndex:(int32_t)index value:(id)value __attribute__((swift_name("insert(index:value:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (BOOL)isNotEmpty __attribute__((swift_name("isNotEmpty()")));
- (void)setI:(int32_t)i t:(id _Nullable)t __attribute__((swift_name("set(i:t:)")));
- (int32_t)size __attribute__((swift_name("size()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property int32_t index __attribute__((swift_name("index")));
@property (readonly) SharedBoolean * _Nullable useIndexedValues __attribute__((swift_name("useIndexedValues")));
@property (readonly, getter=values_) NSArray<id> *values __attribute__((swift_name("values")));
@end

__attribute__((swift_name("KotlinLazy")))
@protocol SharedKotlinLazy
@required
- (BOOL)isInitialized __attribute__((swift_name("isInitialized()")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinLazyThreadSafetyMode")))
@interface SharedKotlinLazyThreadSafetyMode : SharedKotlinEnum<SharedKotlinLazyThreadSafetyMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinLazyThreadSafetyMode *synchronized __attribute__((swift_name("synchronized")));
@property (class, readonly) SharedKotlinLazyThreadSafetyMode *publication __attribute__((swift_name("publication")));
@property (class, readonly) SharedKotlinLazyThreadSafetyMode *none __attribute__((swift_name("none")));
+ (SharedKotlinArray<SharedKotlinLazyThreadSafetyMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinLazyThreadSafetyMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("Koin_coreLogger")))
@interface SharedKoin_coreLogger : SharedBase
- (instancetype)initWithLevel:(SharedKoin_coreLevel *)level __attribute__((swift_name("init(level:)"))) __attribute__((objc_designated_initializer));
- (void)debugMsg:(NSString *)msg __attribute__((swift_name("debug(msg:)")));
- (void)displayLevel:(SharedKoin_coreLevel *)level msg:(NSString *)msg __attribute__((swift_name("display(level:msg:)")));
- (void)errorMsg:(NSString *)msg __attribute__((swift_name("error(msg:)")));
- (void)infoMsg:(NSString *)msg __attribute__((swift_name("info(msg:)")));
- (BOOL)isAtLvl:(SharedKoin_coreLevel *)lvl __attribute__((swift_name("isAt(lvl:)")));
- (void)logLvl:(SharedKoin_coreLevel *)lvl msg:(NSString *(^)(void))msg __attribute__((swift_name("log(lvl:msg:)")));
- (void)logLvl:(SharedKoin_coreLevel *)lvl msg_:(NSString *)msg __attribute__((swift_name("log(lvl:msg_:)")));
- (void)warnMsg:(NSString *)msg __attribute__((swift_name("warn(msg:)")));
@property SharedKoin_coreLevel *level __attribute__((swift_name("level")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreExtensionManager")))
@interface SharedKoin_coreExtensionManager : SharedBase
- (instancetype)initWith_koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(_koin:)"))) __attribute__((objc_designated_initializer));
- (void)close __attribute__((swift_name("close()")));
- (id<SharedKoin_coreKoinExtension>)getExtensionId:(NSString *)id __attribute__((swift_name("getExtension(id:)")));
- (id<SharedKoin_coreKoinExtension> _Nullable)getExtensionOrNullId:(NSString *)id __attribute__((swift_name("getExtensionOrNull(id:)")));
- (void)registerExtensionId:(NSString *)id extension:(id<SharedKoin_coreKoinExtension>)extension __attribute__((swift_name("registerExtension(id:extension:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreInstanceRegistry")))
@interface SharedKoin_coreInstanceRegistry : SharedBase
- (instancetype)initWith_koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(_koin:)"))) __attribute__((objc_designated_initializer));
- (void)saveMappingAllowOverride:(BOOL)allowOverride mapping:(NSString *)mapping factory:(SharedKoin_coreInstanceFactory<id> *)factory logWarning:(BOOL)logWarning __attribute__((swift_name("saveMapping(allowOverride:mapping:factory:logWarning:)")));
- (int32_t)size __attribute__((swift_name("size()")));
@property (readonly) SharedKoin_coreKoin *_koin __attribute__((swift_name("_koin")));
@property (readonly) NSDictionary<NSString *, SharedKoin_coreInstanceFactory<id> *> *instances __attribute__((swift_name("instances")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreOptionRegistry")))
@interface SharedKoin_coreOptionRegistry : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_corePropertyRegistry")))
@interface SharedKoin_corePropertyRegistry : SharedBase
- (instancetype)initWith_koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(_koin:)"))) __attribute__((objc_designated_initializer));
- (void)close __attribute__((swift_name("close()")));
- (void)deletePropertyKey:(NSString *)key __attribute__((swift_name("deleteProperty(key:)")));
- (id _Nullable)getPropertyKey:(NSString *)key __attribute__((swift_name("getProperty(key:)")));
- (void)savePropertiesProperties:(NSDictionary<NSString *, id> *)properties __attribute__((swift_name("saveProperties(properties:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreCoreResolver")))
@interface SharedKoin_coreCoreResolver : SharedBase
- (instancetype)initWith_koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(_koin:)"))) __attribute__((objc_designated_initializer));
- (void)addResolutionExtensionResolutionExtension:(id<SharedKoin_coreResolutionExtension>)resolutionExtension __attribute__((swift_name("addResolutionExtension(resolutionExtension:)")));
- (id _Nullable)resolveFromContextScope:(SharedKoin_coreScope *)scope instanceContext:(SharedKoin_coreResolutionContext *)instanceContext __attribute__((swift_name("resolveFromContext(scope:instanceContext:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreScopeRegistry")))
@interface SharedKoin_coreScopeRegistry : SharedBase
- (instancetype)initWith_koin:(SharedKoin_coreKoin *)_koin __attribute__((swift_name("init(_koin:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKoin_coreScopeRegistryCompanion *companion __attribute__((swift_name("companion")));
- (void)loadScopesModules:(NSSet<SharedKoin_coreModule *> *)modules __attribute__((swift_name("loadScopes(modules:)")));
@property (readonly) SharedKoin_coreScope *rootScope __attribute__((swift_name("rootScope")));
@property (readonly) NSSet<id<SharedKoin_coreQualifier>> *scopeDefinitions __attribute__((swift_name("scopeDefinitions")));
@end

__attribute__((swift_name("KotlinCoroutineContextKey")))
@protocol SharedKotlinCoroutineContextKey
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinContinuation")))
@protocol SharedKotlinContinuation
@required
- (void)resumeWithResult:(id _Nullable)result __attribute__((swift_name("resumeWith(result:)")));
@property (readonly) id<SharedKotlinCoroutineContext> context __attribute__((swift_name("context")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((swift_name("KotlinAbstractCoroutineContextKey")))
@interface SharedKotlinAbstractCoroutineContextKey<B, E> : SharedBase <SharedKotlinCoroutineContextKey>
- (instancetype)initWithBaseKey:(id<SharedKotlinCoroutineContextKey>)baseKey safeCast:(E _Nullable (^)(id<SharedKotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineDispatcher.Key")))
@interface SharedKotlinx_coroutines_coreCoroutineDispatcherKey : SharedKotlinAbstractCoroutineContextKey<id<SharedKotlinContinuationInterceptor>, SharedKotlinx_coroutines_coreCoroutineDispatcher *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithBaseKey:(id<SharedKotlinCoroutineContextKey>)baseKey safeCast:(id<SharedKotlinCoroutineContextElement> _Nullable (^)(id<SharedKotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_coroutines_coreCoroutineDispatcherKey *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreRunnable")))
@protocol SharedKotlinx_coroutines_coreRunnable
@required
- (void)run __attribute__((swift_name("run()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeTimeZone.Companion")))
@interface SharedKotlinx_datetimeTimeZoneCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeTimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinx_datetimeTimeZone *)currentSystemDefault __attribute__((swift_name("currentSystemDefault()")));
- (SharedKotlinx_datetimeTimeZone *)ofZoneId:(NSString *)zoneId __attribute__((swift_name("of(zoneId:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) SharedKotlinx_datetimeFixedOffsetTimeZone *UTC __attribute__((swift_name("UTC")));
@property (readonly) NSSet<NSString *> *availableZoneIds __attribute__((swift_name("availableZoneIds")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateTimeSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateTime")))
@interface SharedKotlinx_datetimeLocalDateTime : SharedBase <SharedKotlinComparable>
- (instancetype)initWithDate:(SharedKotlinx_datetimeLocalDate *)date time:(SharedKotlinx_datetimeLocalTime *)time __attribute__((swift_name("init(date:time:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(int32_t)month day:(int32_t)day hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(year:month:day:hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(SharedKotlinx_datetimeMonth *)month day:(int32_t)day hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond_:(int32_t)nanosecond __attribute__((swift_name("init(year:month:day:hour:minute:second:nanosecond_:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalDateTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinx_datetimeLocalDate *date __attribute__((swift_name("date")));
@property (readonly) int32_t day __attribute__((swift_name("day")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth"))) __attribute__((deprecated("Use the 'day' property instead")));
@property (readonly) SharedKotlinx_datetimeDayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) SharedKotlinx_datetimeMonth *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber"))) __attribute__((deprecated("Use the 'month' property instead")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@property (readonly) SharedKotlinx_datetimeLocalTime *time __attribute__((swift_name("time")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeOverloadMarker")))
@interface SharedKotlinx_datetimeOverloadMarker : SharedBase
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LibraryResourceItem")))
@interface SharedLibraryResourceItem : SharedBase
- (instancetype)initWithQualifiers:(NSSet<id<SharedLibraryQualifier>> *)qualifiers path:(NSString *)path offset:(int64_t)offset size:(int64_t)size __attribute__((swift_name("init(qualifiers:path:offset:size:)"))) __attribute__((objc_designated_initializer));
- (SharedLibraryResourceItem *)doCopyQualifiers:(NSSet<id<SharedLibraryQualifier>> *)qualifiers path:(NSString *)path offset:(int64_t)offset size:(int64_t)size __attribute__((swift_name("doCopy(qualifiers:path:offset:size:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreDisposableHandle")))
@protocol SharedKotlinx_coroutines_coreDisposableHandle
@required
- (void)dispose __attribute__((swift_name("dispose()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreChildHandle")))
@protocol SharedKotlinx_coroutines_coreChildHandle <SharedKotlinx_coroutines_coreDisposableHandle>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (BOOL)childCancelledCause:(SharedKotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
@property (readonly) id<SharedKotlinx_coroutines_coreJob> _Nullable parent __attribute__((swift_name("parent")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreChildJob")))
@protocol SharedKotlinx_coroutines_coreChildJob <SharedKotlinx_coroutines_coreJob>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (void)parentCancelledParentJob:(id<SharedKotlinx_coroutines_coreParentJob>)parentJob __attribute__((swift_name("parentCancelled(parentJob:)")));
@end

__attribute__((swift_name("KotlinSequence")))
@protocol SharedKotlinSequence
@required
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreSelectClause")))
@protocol SharedKotlinx_coroutines_coreSelectClause
@required
@property (readonly) id clauseObject __attribute__((swift_name("clauseObject")));
@property (readonly) SharedKotlinUnit *(^(^ _Nullable onCancellationConstructor)(id<SharedKotlinx_coroutines_coreSelectInstance>, id _Nullable, id _Nullable))(SharedKotlinThrowable *, id _Nullable, id<SharedKotlinCoroutineContext>) __attribute__((swift_name("onCancellationConstructor")));
@property (readonly) id _Nullable (^processResFunc)(id, id _Nullable, id _Nullable) __attribute__((swift_name("processResFunc")));
@property (readonly) void (^regFunc)(id, id<SharedKotlinx_coroutines_coreSelectInstance>, id _Nullable) __attribute__((swift_name("regFunc")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreSelectClause0")))
@protocol SharedKotlinx_coroutines_coreSelectClause0 <SharedKotlinx_coroutines_coreSelectClause>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement.Companion")))
@interface SharedKotlinx_serialization_jsonJsonElementCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_serialization_jsonJsonElementCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("KotlinByteIterator")))
@interface SharedKotlinByteIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedByte *)next __attribute__((swift_name("next()")));
- (int8_t)nextByte __attribute__((swift_name("nextByte()")));
@end

__attribute__((swift_name("KotlinDoubleIterator")))
@interface SharedKotlinDoubleIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedDouble *)next __attribute__((swift_name("next()")));
- (double)nextDouble __attribute__((swift_name("nextDouble()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreKoinDefinition")))
@interface SharedKoin_coreKoinDefinition<R> : SharedBase
- (instancetype)initWithModule:(SharedKoin_coreModule *)module factory:(SharedKoin_coreInstanceFactory<R> *)factory __attribute__((swift_name("init(module:factory:)"))) __attribute__((objc_designated_initializer));
- (SharedKoin_coreKoinDefinition<R> *)doCopyModule:(SharedKoin_coreModule *)module factory:(SharedKoin_coreInstanceFactory<R> *)factory __attribute__((swift_name("doCopy(module:factory:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKoin_coreInstanceFactory<R> *factory __attribute__((swift_name("factory")));
@property (readonly) SharedKoin_coreModule *module __attribute__((swift_name("module")));
@end

__attribute__((swift_name("Koin_coreInstanceFactory")))
@interface SharedKoin_coreInstanceFactory<T> : SharedKoin_coreLockable
- (instancetype)initWithBeanDefinition:(SharedKoin_coreBeanDefinition<T> *)beanDefinition __attribute__((swift_name("init(beanDefinition:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKoin_coreInstanceFactoryCompanion *companion __attribute__((swift_name("companion")));
- (T _Nullable)createContext:(SharedKoin_coreResolutionContext *)context __attribute__((swift_name("create(context:)")));
- (void)dropScope:(SharedKoin_coreScope * _Nullable)scope __attribute__((swift_name("drop(scope:)")));
- (void)dropAll __attribute__((swift_name("dropAll()")));
- (T _Nullable)getContext:(SharedKoin_coreResolutionContext *)context __attribute__((swift_name("get(context:)")));
- (BOOL)isCreatedContext:(SharedKoin_coreResolutionContext * _Nullable)context __attribute__((swift_name("isCreated(context:)")));
@property (readonly) SharedKoin_coreBeanDefinition<T> *beanDefinition __attribute__((swift_name("beanDefinition")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreSingleInstanceFactory")))
@interface SharedKoin_coreSingleInstanceFactory<T> : SharedKoin_coreInstanceFactory<T>
- (instancetype)initWithBeanDefinition:(SharedKoin_coreBeanDefinition<T> *)beanDefinition __attribute__((swift_name("init(beanDefinition:)"))) __attribute__((objc_designated_initializer));
- (T _Nullable)createContext:(SharedKoin_coreResolutionContext *)context __attribute__((swift_name("create(context:)")));
- (void)dropScope:(SharedKoin_coreScope * _Nullable)scope __attribute__((swift_name("drop(scope:)")));
- (void)dropAll __attribute__((swift_name("dropAll()")));
- (T _Nullable)getContext:(SharedKoin_coreResolutionContext *)context __attribute__((swift_name("get(context:)")));
- (BOOL)isCreatedContext:(SharedKoin_coreResolutionContext * _Nullable)context __attribute__((swift_name("isCreated(context:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreScopeDSL")))
@interface SharedKoin_coreScopeDSL : SharedBase
- (instancetype)initWithScopeQualifier:(id<SharedKoin_coreQualifier>)scopeQualifier module:(SharedKoin_coreModule *)module __attribute__((swift_name("init(scopeQualifier:module:)"))) __attribute__((objc_designated_initializer));
- (SharedKoin_coreKoinDefinition<id> *)factoryQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier definition:(id _Nullable (^)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *))definition __attribute__((swift_name("factory(qualifier:definition:)")));
- (SharedKoin_coreKoinDefinition<id> *)scopedQualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier definition:(id _Nullable (^)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *))definition __attribute__((swift_name("scoped(qualifier:definition:)")));
@property (readonly) SharedKoin_coreModule *module __attribute__((swift_name("module")));
@property (readonly) id<SharedKoin_coreQualifier> scopeQualifier __attribute__((swift_name("scopeQualifier")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontWeight")))
@interface SharedUi_textFontWeight : SharedBase <SharedKotlinComparable>
- (instancetype)initWithWeight:(int32_t)weight __attribute__((swift_name("init(weight:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textFontWeightCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedUi_textFontWeight *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t weight __attribute__((swift_name("weight")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextGeometricTransform")))
@interface SharedUi_textTextGeometricTransform : SharedBase
- (instancetype)initWithScaleX:(float)scaleX skewX:(float)skewX __attribute__((swift_name("init(scaleX:skewX:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textTextGeometricTransformCompanion *companion __attribute__((swift_name("companion")));
- (SharedUi_textTextGeometricTransform *)doCopyScaleX:(float)scaleX skewX:(float)skewX __attribute__((swift_name("doCopy(scaleX:skewX:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float scaleX __attribute__((swift_name("scaleX")));
@property (readonly) float skewX __attribute__((swift_name("skewX")));
@end

__attribute__((swift_name("KotlinCollection")))
@protocol SharedKotlinCollection <SharedKotlinIterable>
@required
- (BOOL)containsElement:(id _Nullable)element __attribute__((swift_name("contains(element:)")));
- (BOOL)containsAllElements:(id)elements __attribute__((swift_name("containsAll(elements:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
@property (readonly, getter=size_) int32_t size __attribute__((swift_name("size")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLocaleList")))
@interface SharedUi_textLocaleList : SharedBase <SharedKotlinCollection>
- (instancetype)initWithLocales:(SharedKotlinArray<SharedUi_textLocale *> *)locales __attribute__((swift_name("init(locales:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithLanguageTags:(NSString *)languageTags __attribute__((swift_name("init(languageTags:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithLocaleList:(NSArray<SharedUi_textLocale *> *)localeList __attribute__((swift_name("init(localeList:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textLocaleListCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsElement:(SharedUi_textLocale *)element __attribute__((swift_name("contains(element:)")));
- (BOOL)containsAllElements:(id)elements __attribute__((swift_name("containsAll(elements:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedUi_textLocale *)getI:(int32_t)i __attribute__((swift_name("get(i:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedUi_textLocale *> *localeList __attribute__((swift_name("localeList")));
@property (readonly, getter=size_) int32_t size __attribute__((swift_name("size")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextDecoration")))
@interface SharedUi_textTextDecoration : SharedBase
@property (class, readonly, getter=companion) SharedUi_textTextDecorationCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsOther:(SharedUi_textTextDecoration *)other __attribute__((swift_name("contains(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedUi_textTextDecoration *)plusDecoration:(SharedUi_textTextDecoration *)decoration __attribute__((swift_name("plus(decoration:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t mask __attribute__((swift_name("mask")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_graphicsShadow")))
@interface SharedUi_graphicsShadow : SharedBase
- (instancetype)initWithColor:(uint64_t)color offset:(int64_t)offset blurRadius:(float)blurRadius __attribute__((swift_name("init(color:offset:blurRadius:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_graphicsShadowCompanion *companion __attribute__((swift_name("companion")));
- (SharedUi_graphicsShadow *)doCopyColor:(uint64_t)color offset:(int64_t)offset blurRadius:(float)blurRadius __attribute__((swift_name("doCopy(color:offset:blurRadius:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) float blurRadius __attribute__((swift_name("blurRadius")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) uint64_t color __attribute__((swift_name("color")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) int64_t offset __attribute__((swift_name("offset")));
@end

__attribute__((swift_name("Ui_graphicsDrawStyle")))
@interface SharedUi_graphicsDrawStyle : SharedBase
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextIndent")))
@interface SharedUi_textTextIndent : SharedBase
- (instancetype)initWithFirstLine:(int64_t)firstLine restLine:(int64_t)restLine __attribute__((swift_name("init(firstLine:restLine:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textTextIndentCompanion *companion __attribute__((swift_name("companion")));
- (SharedUi_textTextIndent *)doCopyFirstLine:(int64_t)firstLine restLine:(int64_t)restLine __attribute__((swift_name("doCopy(firstLine:restLine:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t firstLine __attribute__((swift_name("firstLine")));
@property (readonly) int64_t restLine __attribute__((swift_name("restLine")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textPlatformTextStyle")))
@interface SharedUi_textPlatformTextStyle : SharedBase
- (instancetype)initWithTextDecorationLineStyle:(id _Nullable)textDecorationLineStyle __attribute__((swift_name("init(textDecorationLineStyle:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSpanStyle:(SharedUi_textPlatformSpanStyle * _Nullable)spanStyle paragraphStyle:(SharedUi_textPlatformParagraphStyle * _Nullable)paragraphStyle __attribute__((swift_name("init(spanStyle:paragraphStyle:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
@property (readonly) SharedUi_textPlatformParagraphStyle * _Nullable paragraphStyle __attribute__((swift_name("paragraphStyle")));
@property (readonly) SharedUi_textPlatformSpanStyle * _Nullable spanStyle __attribute__((swift_name("spanStyle")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLineHeightStyle")))
@interface SharedUi_textLineHeightStyle : SharedBase
- (instancetype)initWithAlignment:(float)alignment trim:(int32_t)trim __attribute__((swift_name("init(alignment:trim:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithAlignment:(float)alignment trim:(int32_t)trim mode:(int32_t)mode __attribute__((swift_name("init(alignment:trim:mode:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textLineHeightStyleCompanion *companion __attribute__((swift_name("companion")));
- (SharedUi_textLineHeightStyle *)doCopyAlignment:(float)alignment trim:(int32_t)trim mode:(int32_t)mode __attribute__((swift_name("doCopy(alignment:trim:mode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float alignment __attribute__((swift_name("alignment")));
@property (readonly) int32_t mode __attribute__((swift_name("mode")));
@property (readonly) int32_t trim __attribute__((swift_name("trim")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextMotion")))
@interface SharedUi_textTextMotion : SharedBase
@property (class, readonly, getter=companion) SharedUi_textTextMotionCompanion *companion __attribute__((swift_name("companion")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((swift_name("Ui_graphicsBrush")))
@interface SharedUi_graphicsBrush : SharedBase
@property (class, readonly, getter=companion) SharedUi_graphicsBrushCompanion *companion __attribute__((swift_name("companion")));
- (void)applyToSize:(int64_t)size p:(id<SharedUi_graphicsPaint>)p alpha:(float)alpha __attribute__((swift_name("applyTo(size:p:alpha:)")));
@property (readonly) int64_t intrinsicSize __attribute__((swift_name("intrinsicSize")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextStyle.Companion")))
@interface SharedUi_textTextStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textTextStyleCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textTextStyle *Default __attribute__((swift_name("Default")));
@end

__attribute__((swift_name("Ui_textAnnotatedStringAnnotation")))
@protocol SharedUi_textAnnotatedStringAnnotation
@required
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textParagraphStyle")))
@interface SharedUi_textParagraphStyle : SharedBase <SharedUi_textAnnotatedStringAnnotation>
- (instancetype)initWithTextAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformParagraphStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("init(textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)"))) __attribute__((objc_designated_initializer));
- (SharedUi_textParagraphStyle *)doCopyTextAlign:(int32_t)textAlign textDirection:(int32_t)textDirection lineHeight:(int64_t)lineHeight textIndent:(SharedUi_textTextIndent * _Nullable)textIndent platformStyle:(SharedUi_textPlatformParagraphStyle * _Nullable)platformStyle lineHeightStyle:(SharedUi_textLineHeightStyle * _Nullable)lineHeightStyle lineBreak:(int32_t)lineBreak hyphens:(int32_t)hyphens textMotion:(SharedUi_textTextMotion * _Nullable)textMotion __attribute__((swift_name("doCopy(textAlign:textDirection:lineHeight:textIndent:platformStyle:lineHeightStyle:lineBreak:hyphens:textMotion:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textParagraphStyle *)mergeOther:(SharedUi_textParagraphStyle * _Nullable)other __attribute__((swift_name("merge(other:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textParagraphStyle *)plusOther:(SharedUi_textParagraphStyle *)other __attribute__((swift_name("plus(other:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id _Nullable deprecated_boxing_hyphens __attribute__((swift_name("deprecated_boxing_hyphens"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_lineBreak __attribute__((swift_name("deprecated_boxing_lineBreak"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_textAlign __attribute__((swift_name("deprecated_boxing_textAlign"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) id _Nullable deprecated_boxing_textDirection __attribute__((swift_name("deprecated_boxing_textDirection"))) __attribute__((deprecated("Kept for backwards compatibility.")));
@property (readonly) int32_t hyphens __attribute__((swift_name("hyphens")));
@property (readonly) int32_t lineBreak __attribute__((swift_name("lineBreak")));
@property (readonly) int64_t lineHeight __attribute__((swift_name("lineHeight")));
@property (readonly) SharedUi_textLineHeightStyle * _Nullable lineHeightStyle __attribute__((swift_name("lineHeightStyle")));
@property (readonly) SharedUi_textPlatformParagraphStyle * _Nullable platformStyle __attribute__((swift_name("platformStyle")));
@property (readonly) int32_t textAlign __attribute__((swift_name("textAlign")));
@property (readonly) int32_t textDirection __attribute__((swift_name("textDirection")));
@property (readonly) SharedUi_textTextIndent * _Nullable textIndent __attribute__((swift_name("textIndent")));
@property (readonly) SharedUi_textTextMotion * _Nullable textMotion __attribute__((swift_name("textMotion")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textSpanStyle")))
@interface SharedUi_textSpanStyle : SharedBase <SharedUi_textAnnotatedStringAnnotation>
- (instancetype)initWithColor:(uint64_t)color fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow platformStyle:(SharedUi_textPlatformSpanStyle * _Nullable)platformStyle drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle __attribute__((swift_name("init(color:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:platformStyle:drawStyle:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithBrush:(SharedUi_graphicsBrush * _Nullable)brush alpha:(float)alpha fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow platformStyle:(SharedUi_textPlatformSpanStyle * _Nullable)platformStyle drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle __attribute__((swift_name("init(brush:alpha:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:platformStyle:drawStyle:)"))) __attribute__((objc_designated_initializer));
- (SharedUi_textSpanStyle *)doCopyColor:(uint64_t)color fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow platformStyle:(SharedUi_textPlatformSpanStyle * _Nullable)platformStyle drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle __attribute__((swift_name("doCopy(color:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:platformStyle:drawStyle:)")));
- (SharedUi_textSpanStyle *)doCopyBrush:(SharedUi_graphicsBrush * _Nullable)brush alpha:(float)alpha fontSize:(int64_t)fontSize fontWeight:(SharedUi_textFontWeight * _Nullable)fontWeight fontStyle:(id _Nullable)fontStyle fontSynthesis:(id _Nullable)fontSynthesis fontFamily:(SharedUi_textFontFamily * _Nullable)fontFamily fontFeatureSettings:(NSString * _Nullable)fontFeatureSettings letterSpacing:(int64_t)letterSpacing baselineShift:(id _Nullable)baselineShift textGeometricTransform:(SharedUi_textTextGeometricTransform * _Nullable)textGeometricTransform localeList:(SharedUi_textLocaleList * _Nullable)localeList background:(uint64_t)background textDecoration:(SharedUi_textTextDecoration * _Nullable)textDecoration shadow:(SharedUi_graphicsShadow * _Nullable)shadow platformStyle:(SharedUi_textPlatformSpanStyle * _Nullable)platformStyle drawStyle:(SharedUi_graphicsDrawStyle * _Nullable)drawStyle __attribute__((swift_name("doCopy(brush:alpha:fontSize:fontWeight:fontStyle:fontSynthesis:fontFamily:fontFeatureSettings:letterSpacing:baselineShift:textGeometricTransform:localeList:background:textDecoration:shadow:platformStyle:drawStyle:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textSpanStyle *)mergeOther:(SharedUi_textSpanStyle * _Nullable)other __attribute__((swift_name("merge(other:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_textSpanStyle *)plusOther:(SharedUi_textSpanStyle *)other __attribute__((swift_name("plus(other:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float alpha __attribute__((swift_name("alpha")));
@property (readonly) uint64_t background __attribute__((swift_name("background")));
@property (readonly) id _Nullable baselineShift __attribute__((swift_name("baselineShift")));
@property (readonly) SharedUi_graphicsBrush * _Nullable brush __attribute__((swift_name("brush")));
@property (readonly) uint64_t color __attribute__((swift_name("color")));
@property (readonly) SharedUi_graphicsDrawStyle * _Nullable drawStyle __attribute__((swift_name("drawStyle")));
@property (readonly) SharedUi_textFontFamily * _Nullable fontFamily __attribute__((swift_name("fontFamily")));
@property (readonly) NSString * _Nullable fontFeatureSettings __attribute__((swift_name("fontFeatureSettings")));
@property (readonly) int64_t fontSize __attribute__((swift_name("fontSize")));
@property (readonly) id _Nullable fontStyle __attribute__((swift_name("fontStyle")));
@property (readonly) id _Nullable fontSynthesis __attribute__((swift_name("fontSynthesis")));
@property (readonly) SharedUi_textFontWeight * _Nullable fontWeight __attribute__((swift_name("fontWeight")));
@property (readonly) int64_t letterSpacing __attribute__((swift_name("letterSpacing")));
@property (readonly) SharedUi_textLocaleList * _Nullable localeList __attribute__((swift_name("localeList")));
@property (readonly) SharedUi_textPlatformSpanStyle * _Nullable platformStyle __attribute__((swift_name("platformStyle")));
@property (readonly) SharedUi_graphicsShadow * _Nullable shadow __attribute__((swift_name("shadow")));
@property (readonly) SharedUi_textTextDecoration * _Nullable textDecoration __attribute__((swift_name("textDecoration")));
@property (readonly) SharedUi_textTextGeometricTransform * _Nullable textGeometricTransform __attribute__((swift_name("textGeometricTransform")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontFamily.Companion")))
@interface SharedUi_textFontFamilyCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textFontFamilyCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textGenericFontFamily *Cursive __attribute__((swift_name("Cursive")));
@property (readonly) SharedUi_textSystemFontFamily *Default __attribute__((swift_name("Default")));
@property (readonly) SharedUi_textGenericFontFamily *Monospace __attribute__((swift_name("Monospace")));
@property (readonly) SharedUi_textGenericFontFamily *SansSerif __attribute__((swift_name("SansSerif")));
@property (readonly) SharedUi_textGenericFontFamily *Serif __attribute__((swift_name("Serif")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeEncoder")))
@protocol SharedKotlinx_serialization_coreCompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializersModule")))
@interface SharedKotlinx_serialization_coreSerializersModule : SharedBase

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<SharedKotlinx_serialization_coreSerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreKSerializer> _Nullable)getContextualKClass:(id<SharedKotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end

__attribute__((swift_name("KotlinAnnotation")))
@protocol SharedKotlinAnnotation
@required
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialKind")))
@interface SharedKotlinx_serialization_coreSerialKind : SharedBase
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeDecoder")))
@protocol SharedKotlinx_serialization_coreCompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface SharedKotlinNothing : SharedBase
@end

__attribute__((swift_name("Koin_coreScopeCallback")))
@protocol SharedKoin_coreScopeCallback
@required
- (void)onScopeCloseScope:(SharedKoin_coreScope *)scope __attribute__((swift_name("onScopeClose(scope:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreLevel")))
@interface SharedKoin_coreLevel : SharedKotlinEnum<SharedKoin_coreLevel *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKoin_coreLevel *debug __attribute__((swift_name("debug")));
@property (class, readonly) SharedKoin_coreLevel *info __attribute__((swift_name("info")));
@property (class, readonly) SharedKoin_coreLevel *warning __attribute__((swift_name("warning")));
@property (class, readonly) SharedKoin_coreLevel *error __attribute__((swift_name("error")));
@property (class, readonly) SharedKoin_coreLevel *none __attribute__((swift_name("none")));
+ (SharedKotlinArray<SharedKoin_coreLevel *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKoin_coreLevel *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("Koin_coreKoinExtension")))
@protocol SharedKoin_coreKoinExtension
@required
- (void)onClose __attribute__((swift_name("onClose()")));
- (void)onRegisterKoin:(SharedKoin_coreKoin *)koin __attribute__((swift_name("onRegister(koin:)")));
@end

__attribute__((swift_name("Koin_coreResolutionExtension")))
@protocol SharedKoin_coreResolutionExtension
@required
- (id _Nullable)resolveScope:(SharedKoin_coreScope *)scope instanceContext:(SharedKoin_coreResolutionContext *)instanceContext __attribute__((swift_name("resolve(scope:instanceContext:)")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreResolutionContext")))
@interface SharedKoin_coreResolutionContext : SharedBase
- (instancetype)initWithLogger:(SharedKoin_coreLogger *)logger scope:(SharedKoin_coreScope *)scope clazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier parameters:(SharedKoin_coreParametersHolder * _Nullable)parameters __attribute__((swift_name("init(logger:scope:clazz:qualifier:parameters:)"))) __attribute__((objc_designated_initializer));
- (SharedKoin_coreResolutionContext *)doNewContextForScopeS:(SharedKoin_coreScope *)s __attribute__((swift_name("doNewContextForScope(s:)")));
@property (readonly) id<SharedKotlinKClass> clazz __attribute__((swift_name("clazz")));
@property (readonly) NSString *debugTag __attribute__((swift_name("debugTag")));
@property (readonly) SharedKoin_coreLogger *logger __attribute__((swift_name("logger")));
@property (readonly) SharedKoin_coreParametersHolder * _Nullable parameters __attribute__((swift_name("parameters")));
@property (readonly) id<SharedKoin_coreQualifier> _Nullable qualifier __attribute__((swift_name("qualifier")));
@property (readonly) SharedKoin_coreScope *scope __attribute__((swift_name("scope")));
@property SharedKoin_coreTypeQualifier * _Nullable scopeArchetype __attribute__((swift_name("scopeArchetype")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreScopeRegistry.Companion")))
@interface SharedKoin_coreScopeRegistryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKoin_coreScopeRegistryCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/FixedOffsetTimeZoneSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeFixedOffsetTimeZone")))
@interface SharedKotlinx_datetimeFixedOffsetTimeZone : SharedKotlinx_datetimeTimeZone
- (instancetype)initWithOffset:(SharedKotlinx_datetimeUtcOffset *)offset __attribute__((swift_name("init(offset:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedKotlinx_datetimeUtcOffset *offset __attribute__((swift_name("offset")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds"))) __attribute__((deprecated("Use offset.totalSeconds")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDate")))
@interface SharedKotlinx_datetimeLocalDate : SharedBase <SharedKotlinComparable>
- (instancetype)initWithYear:(int32_t)year month:(int32_t)month day:(int32_t)day __attribute__((swift_name("init(year:month:day:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(SharedKotlinx_datetimeMonth *)month day_:(int32_t)day __attribute__((swift_name("init(year:month:day_:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalDate *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKotlinx_datetimeLocalDateRange *)rangeToThat:(SharedKotlinx_datetimeLocalDate *)that __attribute__((swift_name("rangeTo(that:)")));
- (SharedKotlinx_datetimeLocalDateRange *)rangeUntilThat:(SharedKotlinx_datetimeLocalDate *)that __attribute__((swift_name("rangeUntil(that:)")));
- (int64_t)toEpochDays __attribute__((swift_name("toEpochDays()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t day __attribute__((swift_name("day")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth"))) __attribute__((deprecated("Use the 'day' property instead")));
@property (readonly) SharedKotlinx_datetimeDayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) SharedKotlinx_datetimeMonth *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber"))) __attribute__((deprecated("Use the 'month' property instead")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalTimeSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalTime")))
@interface SharedKotlinx_datetimeLocalTime : SharedBase <SharedKotlinComparable>
- (instancetype)initWithHour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)toMillisecondOfDay __attribute__((swift_name("toMillisecondOfDay()")));
- (int64_t)toNanosecondOfDay __attribute__((swift_name("toNanosecondOfDay()")));
- (int32_t)toSecondOfDay __attribute__((swift_name("toSecondOfDay()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonth")))
@interface SharedKotlinx_datetimeMonth : SharedKotlinEnum<SharedKotlinx_datetimeMonth *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimeMonth *january __attribute__((swift_name("january")));
@property (class, readonly) SharedKotlinx_datetimeMonth *february __attribute__((swift_name("february")));
@property (class, readonly) SharedKotlinx_datetimeMonth *march __attribute__((swift_name("march")));
@property (class, readonly) SharedKotlinx_datetimeMonth *april __attribute__((swift_name("april")));
@property (class, readonly) SharedKotlinx_datetimeMonth *may __attribute__((swift_name("may")));
@property (class, readonly) SharedKotlinx_datetimeMonth *june __attribute__((swift_name("june")));
@property (class, readonly) SharedKotlinx_datetimeMonth *july __attribute__((swift_name("july")));
@property (class, readonly) SharedKotlinx_datetimeMonth *august __attribute__((swift_name("august")));
@property (class, readonly) SharedKotlinx_datetimeMonth *september __attribute__((swift_name("september")));
@property (class, readonly) SharedKotlinx_datetimeMonth *october __attribute__((swift_name("october")));
@property (class, readonly) SharedKotlinx_datetimeMonth *november __attribute__((swift_name("november")));
@property (class, readonly) SharedKotlinx_datetimeMonth *december __attribute__((swift_name("december")));
+ (SharedKotlinArray<SharedKotlinx_datetimeMonth *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimeMonth *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateTime.Companion")))
@interface SharedKotlinx_datetimeLocalDateTimeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBuilder:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime>))builder __attribute__((swift_name("Format(builder:)")));
- (SharedKotlinx_datetimeLocalDateTime *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeek")))
@interface SharedKotlinx_datetimeDayOfWeek : SharedKotlinEnum<SharedKotlinx_datetimeDayOfWeek *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *monday __attribute__((swift_name("monday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *tuesday __attribute__((swift_name("tuesday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *wednesday __attribute__((swift_name("wednesday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *thursday __attribute__((swift_name("thursday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *friday __attribute__((swift_name("friday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *saturday __attribute__((swift_name("saturday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *sunday __attribute__((swift_name("sunday")));
+ (SharedKotlinArray<SharedKotlinx_datetimeDayOfWeek *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimeDayOfWeek *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("LibraryQualifier")))
@protocol SharedLibraryQualifier
@required
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreParentJob")))
@protocol SharedKotlinx_coroutines_coreParentJob <SharedKotlinx_coroutines_coreJob>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (SharedKotlinCancellationException *)getChildJobCancellationCause __attribute__((swift_name("getChildJobCancellationCause()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreSelectInstance")))
@protocol SharedKotlinx_coroutines_coreSelectInstance
@required
- (void)disposeOnCompletionDisposableHandle:(id<SharedKotlinx_coroutines_coreDisposableHandle>)disposableHandle __attribute__((swift_name("disposeOnCompletion(disposableHandle:)")));
- (void)selectInRegistrationPhaseInternalResult:(id _Nullable)internalResult __attribute__((swift_name("selectInRegistrationPhase(internalResult:)")));
- (BOOL)trySelectClauseObject:(id)clauseObject result:(id _Nullable)result __attribute__((swift_name("trySelect(clauseObject:result:)")));
@property (readonly) id<SharedKotlinCoroutineContext> context __attribute__((swift_name("context")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreBeanDefinition")))
@interface SharedKoin_coreBeanDefinition<T> : SharedBase
- (instancetype)initWithScopeQualifier:(id<SharedKoin_coreQualifier>)scopeQualifier primaryType:(id<SharedKotlinKClass>)primaryType qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier definition:(T _Nullable (^)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *))definition kind:(SharedKoin_coreKind *)kind secondaryTypes:(NSArray<id<SharedKotlinKClass>> *)secondaryTypes __attribute__((swift_name("init(scopeQualifier:primaryType:qualifier:definition:kind:secondaryTypes:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (BOOL)hasTypeClazz:(id<SharedKotlinKClass>)clazz __attribute__((swift_name("hasType(clazz:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isClazz:(id<SharedKotlinKClass>)clazz qualifier:(id<SharedKoin_coreQualifier> _Nullable)qualifier scopeDefinition:(id<SharedKoin_coreQualifier>)scopeDefinition __attribute__((swift_name("is(clazz:qualifier:scopeDefinition:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property SharedKoin_coreCallbacks<T> *callbacks __attribute__((swift_name("callbacks")));
@property (readonly) T _Nullable (^definition)(SharedKoin_coreScope *, SharedKoin_coreParametersHolder *) __attribute__((swift_name("definition")));
@property (readonly) SharedKoin_coreKind *kind __attribute__((swift_name("kind")));
@property (readonly) id<SharedKotlinKClass> primaryType __attribute__((swift_name("primaryType")));
@property id<SharedKoin_coreQualifier> _Nullable qualifier __attribute__((swift_name("qualifier")));
@property (readonly) id<SharedKoin_coreQualifier> scopeQualifier __attribute__((swift_name("scopeQualifier")));
@property NSArray<id<SharedKotlinKClass>> *secondaryTypes __attribute__((swift_name("secondaryTypes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreInstanceFactoryCompanion")))
@interface SharedKoin_coreInstanceFactoryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKoin_coreInstanceFactoryCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *ERROR_SEPARATOR __attribute__((swift_name("ERROR_SEPARATOR")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontWeight.Companion")))
@interface SharedUi_textFontWeightCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textFontWeightCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Black __attribute__((swift_name("Black")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Bold __attribute__((swift_name("Bold")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *ExtraBold __attribute__((swift_name("ExtraBold")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *ExtraLight __attribute__((swift_name("ExtraLight")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Light __attribute__((swift_name("Light")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Medium __attribute__((swift_name("Medium")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Normal __attribute__((swift_name("Normal")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *SemiBold __attribute__((swift_name("SemiBold")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *Thin __attribute__((swift_name("Thin")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W100 __attribute__((swift_name("W100")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W200 __attribute__((swift_name("W200")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W300 __attribute__((swift_name("W300")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W400 __attribute__((swift_name("W400")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W500 __attribute__((swift_name("W500")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W600 __attribute__((swift_name("W600")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W700 __attribute__((swift_name("W700")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W800 __attribute__((swift_name("W800")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textFontWeight *W900 __attribute__((swift_name("W900")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextGeometricTransform.Companion")))
@interface SharedUi_textTextGeometricTransformCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textTextGeometricTransformCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLocale")))
@interface SharedUi_textLocale : SharedBase
- (instancetype)initWithLanguageTag:(NSString *)languageTag __attribute__((swift_name("init(languageTag:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textLocaleCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)toLanguageTag __attribute__((swift_name("toLanguageTag()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *language __attribute__((swift_name("language")));
@property (readonly) NSLocale *platformLocale __attribute__((swift_name("platformLocale")));
@property (readonly) NSString *region __attribute__((swift_name("region")));
@property (readonly) NSString *script __attribute__((swift_name("script")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLocaleList.Companion")))
@interface SharedUi_textLocaleListCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textLocaleListCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textLocaleList *Empty __attribute__((swift_name("Empty")));
@property (readonly) SharedUi_textLocaleList *current __attribute__((swift_name("current")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextDecoration.Companion")))
@interface SharedUi_textTextDecorationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textTextDecorationCompanion *shared __attribute__((swift_name("shared")));
- (SharedUi_textTextDecoration *)combineDecorations:(NSArray<SharedUi_textTextDecoration *> *)decorations __attribute__((swift_name("combine(decorations:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textTextDecoration *LineThrough __attribute__((swift_name("LineThrough")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textTextDecoration *None __attribute__((swift_name("None")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textTextDecoration *Underline __attribute__((swift_name("Underline")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_graphicsShadow.Companion")))
@interface SharedUi_graphicsShadowCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_graphicsShadowCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_graphicsShadow *None __attribute__((swift_name("None")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextIndent.Companion")))
@interface SharedUi_textTextIndentCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textTextIndentCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
@property (readonly) SharedUi_textTextIndent *None __attribute__((swift_name("None")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textPlatformSpanStyle")))
@interface SharedUi_textPlatformSpanStyle : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithTextDecorationLineStyle:(id _Nullable)textDecorationLineStyle __attribute__((swift_name("init(textDecorationLineStyle:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textPlatformSpanStyleCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedUi_textPlatformSpanStyle *)mergeOther:(SharedUi_textPlatformSpanStyle * _Nullable)other __attribute__((swift_name("merge(other:)")));
@property (readonly) id _Nullable textDecorationLineStyle __attribute__((swift_name("textDecorationLineStyle")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textPlatformParagraphStyle")))
@interface SharedUi_textPlatformParagraphStyle : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithFontRasterizationSettings:(SharedUi_textFontRasterizationSettings *)fontRasterizationSettings __attribute__((swift_name("init(fontRasterizationSettings:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textPlatformParagraphStyleCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedUi_textPlatformParagraphStyle *)mergeOther:(SharedUi_textPlatformParagraphStyle * _Nullable)other __attribute__((swift_name("merge(other:)")));
@property (readonly) SharedUi_textFontRasterizationSettings *fontRasterizationSettings __attribute__((swift_name("fontRasterizationSettings")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLineHeightStyle.Companion")))
@interface SharedUi_textLineHeightStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textLineHeightStyleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textLineHeightStyle *Default __attribute__((swift_name("Default")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textTextMotion.Companion")))
@interface SharedUi_textTextMotionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textTextMotionCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textTextMotion *Animated __attribute__((swift_name("Animated")));
@property (readonly) SharedUi_textTextMotion *Static __attribute__((swift_name("Static")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_graphicsBrush.Companion")))
@interface SharedUi_graphicsBrushCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_graphicsBrushCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)horizontalGradientColorStops:(SharedKotlinArray<SharedKotlinPair<SharedFloat *, id> *> *)colorStops startX:(float)startX endX:(float)endX tileMode:(int32_t)tileMode __attribute__((swift_name("horizontalGradient(colorStops:startX:endX:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)horizontalGradientColors:(NSArray<id> *)colors startX:(float)startX endX:(float)endX tileMode:(int32_t)tileMode __attribute__((swift_name("horizontalGradient(colors:startX:endX:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)linearGradientColorStops:(SharedKotlinArray<SharedKotlinPair<SharedFloat *, id> *> *)colorStops start:(int64_t)start end:(int64_t)end tileMode:(int32_t)tileMode __attribute__((swift_name("linearGradient(colorStops:start:end:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)linearGradientColors:(NSArray<id> *)colors start:(int64_t)start end:(int64_t)end tileMode:(int32_t)tileMode __attribute__((swift_name("linearGradient(colors:start:end:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)radialGradientColorStops:(SharedKotlinArray<SharedKotlinPair<SharedFloat *, id> *> *)colorStops center:(int64_t)center radius:(float)radius tileMode:(int32_t)tileMode __attribute__((swift_name("radialGradient(colorStops:center:radius:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)radialGradientColors:(NSArray<id> *)colors center:(int64_t)center radius:(float)radius tileMode:(int32_t)tileMode __attribute__((swift_name("radialGradient(colors:center:radius:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)sweepGradientColorStops:(SharedKotlinArray<SharedKotlinPair<SharedFloat *, id> *> *)colorStops center:(int64_t)center __attribute__((swift_name("sweepGradient(colorStops:center:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)sweepGradientColors:(NSArray<id> *)colors center:(int64_t)center __attribute__((swift_name("sweepGradient(colors:center:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)verticalGradientColorStops:(SharedKotlinArray<SharedKotlinPair<SharedFloat *, id> *> *)colorStops startY:(float)startY endY:(float)endY tileMode:(int32_t)tileMode __attribute__((swift_name("verticalGradient(colorStops:startY:endY:tileMode:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsBrush *)verticalGradientColors:(NSArray<id> *)colors startY:(float)startY endY:(float)endY tileMode:(int32_t)tileMode __attribute__((swift_name("verticalGradient(colors:startY:endY:tileMode:)")));
@end

__attribute__((swift_name("Ui_graphicsPaint")))
@protocol SharedUi_graphicsPaint
@required
- (SharedSkikoPaint *)asFrameworkPaint __attribute__((swift_name("asFrameworkPaint()")));
@property float alpha __attribute__((swift_name("alpha")));
@property int32_t blendMode __attribute__((swift_name("blendMode")));
@property uint64_t color __attribute__((swift_name("color")));
@property SharedUi_graphicsColorFilter * _Nullable colorFilter __attribute__((swift_name("colorFilter")));
@property int32_t filterQuality __attribute__((swift_name("filterQuality")));
@property BOOL isAntiAlias __attribute__((swift_name("isAntiAlias")));
@property id<SharedUi_graphicsPathEffect> _Nullable pathEffect __attribute__((swift_name("pathEffect")));
@property SharedSkikoShader * _Nullable shader __attribute__((swift_name("shader")));
@property int32_t strokeCap __attribute__((swift_name("strokeCap")));
@property int32_t strokeJoin __attribute__((swift_name("strokeJoin")));
@property float strokeMiterLimit __attribute__((swift_name("strokeMiterLimit")));
@property float strokeWidth __attribute__((swift_name("strokeWidth")));
@property int32_t style __attribute__((swift_name("style")));
@end

__attribute__((swift_name("Ui_textSystemFontFamily")))
@interface SharedUi_textSystemFontFamily : SharedUi_textFontFamily
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textGenericFontFamily")))
@interface SharedUi_textGenericFontFamily : SharedUi_textSystemFontFamily
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModuleCollector")))
@protocol SharedKotlinx_serialization_coreSerializersModuleCollector
@required
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass provider:(id<SharedKotlinx_serialization_coreKSerializer> (^)(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass serializer:(id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<SharedKotlinKClass>)baseClass actualClass:(id<SharedKotlinKClass>)actualClass actualSerializer:(id<SharedKotlinx_serialization_coreKSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultSerializerProvider:(id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/UtcOffsetSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeUtcOffset")))
@interface SharedKotlinx_datetimeUtcOffset : SharedBase
@property (class, readonly, getter=companion) SharedKotlinx_datetimeUtcOffsetCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeFixedOffsetTimeZone.Companion")))
@interface SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDate.Companion")))
@interface SharedKotlinx_datetimeLocalDateCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBlock:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithDate>))block __attribute__((swift_name("Format(block:)")));
- (SharedKotlinx_datetimeLocalDate *)fromEpochDaysEpochDays:(int32_t)epochDays __attribute__((swift_name("fromEpochDays(epochDays:)")));
- (SharedKotlinx_datetimeLocalDate *)fromEpochDaysEpochDays_:(int64_t)epochDays __attribute__((swift_name("fromEpochDays(epochDays_:)")));
- (SharedKotlinx_datetimeLocalDate *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("Kotlinx_datetimeLocalDateProgression")))
@interface SharedKotlinx_datetimeLocalDateProgression : SharedBase <SharedKotlinCollection>
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateProgressionCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsElement:(SharedKotlinx_datetimeLocalDate *)element __attribute__((swift_name("contains(element:)")));
- (BOOL)containsAllElements:(id)elements __attribute__((swift_name("containsAll(elements:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinx_datetimeLocalDate *first __attribute__((swift_name("first")));
@property (readonly) SharedKotlinx_datetimeLocalDate *last __attribute__((swift_name("last")));
@property (readonly, getter=size_) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinClosedRange")))
@protocol SharedKotlinClosedRange
@required
- (BOOL)containsValue:(id)value __attribute__((swift_name("contains(value:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
@property (readonly) id endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) id start __attribute__((swift_name("start")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
__attribute__((swift_name("KotlinOpenEndRange")))
@protocol SharedKotlinOpenEndRange
@required
- (BOOL)containsValue_:(id)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
@property (readonly) id endExclusive __attribute__((swift_name("endExclusive")));
@property (readonly, getter=start_) id start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateRange")))
@interface SharedKotlinx_datetimeLocalDateRange : SharedKotlinx_datetimeLocalDateProgression <SharedKotlinClosedRange, SharedKotlinOpenEndRange>
- (instancetype)initWithStart:(SharedKotlinx_datetimeLocalDate *)start endInclusive:(SharedKotlinx_datetimeLocalDate *)endInclusive __attribute__((swift_name("init(start:endInclusive:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateRangeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsElement:(SharedKotlinx_datetimeLocalDate *)element __attribute__((swift_name("contains(element:)")));
- (BOOL)containsValue:(SharedKotlinx_datetimeLocalDate *)element __attribute__((swift_name("contains(value:)")));
- (BOOL)containsValue_:(SharedKotlinx_datetimeLocalDate *)element __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinx_datetimeLocalDate *endExclusive __attribute__((swift_name("endExclusive"))) __attribute__((deprecated("This throws an exception if the exclusive end if not inside the platform-specific boundaries for LocalDate. The 'endInclusive' property does not throw and should be preferred.")));
@property (readonly) SharedKotlinx_datetimeLocalDate *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) SharedKotlinx_datetimeLocalDate *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalTime.Companion")))
@interface SharedKotlinx_datetimeLocalTimeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBuilder:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithTime>))builder __attribute__((swift_name("Format(builder:)")));
- (SharedKotlinx_datetimeLocalTime *)fromMillisecondOfDayMillisecondOfDay:(int32_t)millisecondOfDay __attribute__((swift_name("fromMillisecondOfDay(millisecondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)fromNanosecondOfDayNanosecondOfDay:(int64_t)nanosecondOfDay __attribute__((swift_name("fromNanosecondOfDay(nanosecondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)fromSecondOfDaySecondOfDay:(int32_t)secondOfDay __attribute__((swift_name("fromSecondOfDay(secondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormat")))
@protocol SharedKotlinx_datetimeDateTimeFormat
@required
- (NSString *)formatValue:(id _Nullable)value __attribute__((swift_name("format(value:)")));
- (id<SharedKotlinAppendable>)formatToAppendable:(id<SharedKotlinAppendable>)appendable value:(id _Nullable)value __attribute__((swift_name("formatTo(appendable:value:)")));
- (id _Nullable)parseInput:(id)input __attribute__((swift_name("parse(input:)")));
- (id _Nullable)parseOrNullInput:(id)input __attribute__((swift_name("parseOrNull(input:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilder")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilder
@required
- (void)charsValue:(NSString *)value __attribute__((swift_name("chars(value:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithYearMonth")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithYearMonth <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)monthNameNames:(SharedKotlinx_datetimeMonthNames *)names __attribute__((swift_name("monthName(names:)")));
- (void)monthNumberPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("monthNumber(padding:)")));
- (void)yearPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("year(padding:)")));
- (void)yearMonthFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("yearMonth(format:)")));
- (void)yearTwoDigitsBaseYear:(int32_t)baseYear __attribute__((swift_name("yearTwoDigits(baseYear:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithDate")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithDate <SharedKotlinx_datetimeDateTimeFormatBuilderWithYearMonth>
@required
- (void)dateFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("date(format:)")));
- (void)dayPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("day(padding:)")));
- (void)dayOfMonthPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("dayOfMonth(padding:)"))) __attribute__((deprecated("Use 'day' instead")));
- (void)dayOfWeekNames:(SharedKotlinx_datetimeDayOfWeekNames *)names __attribute__((swift_name("dayOfWeek(names:)")));
- (void)dayOfYearPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("dayOfYear(padding:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithTime")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithTime <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)amPmHourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("amPmHour(padding:)")));
- (void)amPmMarkerAm:(NSString *)am pm:(NSString *)pm __attribute__((swift_name("amPmMarker(am:pm:)")));
- (void)hourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("hour(padding:)")));
- (void)minutePadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("minute(padding:)")));
- (void)secondPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("second(padding:)")));
- (void)secondFractionFixedLength:(int32_t)fixedLength __attribute__((swift_name("secondFraction(fixedLength:)")));
- (void)secondFractionMinLength:(int32_t)minLength maxLength:(int32_t)maxLength __attribute__((swift_name("secondFraction(minLength:maxLength:)")));
- (void)timeFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("time(format:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithDateTime")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime <SharedKotlinx_datetimeDateTimeFormatBuilderWithDate, SharedKotlinx_datetimeDateTimeFormatBuilderWithTime>
@required
- (void)dateTimeFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("dateTime(format:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreKind")))
@interface SharedKoin_coreKind : SharedKotlinEnum<SharedKoin_coreKind *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKoin_coreKind *singleton __attribute__((swift_name("singleton")));
@property (class, readonly) SharedKoin_coreKind *factory __attribute__((swift_name("factory")));
@property (class, readonly) SharedKoin_coreKind *scoped __attribute__((swift_name("scoped")));
+ (SharedKotlinArray<SharedKoin_coreKind *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKoin_coreKind *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Koin_coreCallbacks")))
@interface SharedKoin_coreCallbacks<T> : SharedBase
- (instancetype)initWithOnClose:(void (^ _Nullable)(T _Nullable))onClose __attribute__((swift_name("init(onClose:)"))) __attribute__((objc_designated_initializer));
- (SharedKoin_coreCallbacks<T> *)doCopyOnClose:(void (^ _Nullable)(T _Nullable))onClose __attribute__((swift_name("doCopy(onClose:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) void (^ _Nullable onClose)(T _Nullable) __attribute__((swift_name("onClose")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textLocale.Companion")))
@interface SharedUi_textLocaleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textLocaleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textLocale *current __attribute__((swift_name("current")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textPlatformSpanStyle.Companion")))
@interface SharedUi_textPlatformSpanStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textPlatformSpanStyleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textPlatformSpanStyle *Default __attribute__((swift_name("Default")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontRasterizationSettings")))
@interface SharedUi_textFontRasterizationSettings : SharedBase
- (instancetype)initWithSmoothing:(SharedUi_textFontSmoothing *)smoothing hinting:(SharedUi_textFontHinting *)hinting subpixelPositioning:(BOOL)subpixelPositioning autoHintingForced:(BOOL)autoHintingForced __attribute__((swift_name("init(smoothing:hinting:subpixelPositioning:autoHintingForced:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUi_textFontRasterizationSettingsCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL autoHintingForced __attribute__((swift_name("autoHintingForced")));
@property (readonly) SharedUi_textFontHinting *hinting __attribute__((swift_name("hinting")));
@property (readonly) SharedUi_textFontSmoothing *smoothing __attribute__((swift_name("smoothing")));
@property (readonly) BOOL subpixelPositioning __attribute__((swift_name("subpixelPositioning")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textPlatformParagraphStyle.Companion")))
@interface SharedUi_textPlatformParagraphStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textPlatformParagraphStyleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textPlatformParagraphStyle *Default __attribute__((swift_name("Default")));
@end

__attribute__((swift_name("SkikoNative")))
@interface SharedSkikoNative : SharedBase
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoNativeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("SkikoManaged")))
@interface SharedSkikoManaged : SharedSkikoNative
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (void)close __attribute__((swift_name("close()")));
@property (readonly) BOOL isClosed __attribute__((swift_name("isClosed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPaint")))
@interface SharedSkikoPaint : SharedSkikoManaged
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPaintCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)hasNothingToDraw __attribute__((swift_name("hasNothingToDraw()")));
- (SharedSkikoPaint *)makeClone __attribute__((swift_name("makeClone()")));
- (SharedSkikoPaint *)reset __attribute__((swift_name("reset()")));
- (SharedSkikoPaint *)setARGBA:(int32_t)a r:(int32_t)r g:(int32_t)g b:(int32_t)b __attribute__((swift_name("setARGB(a:r:g:b:)")));
- (SharedSkikoPaint *)setAlphafA:(float)a __attribute__((swift_name("setAlphaf(a:)")));
- (SharedSkikoPaint *)setColor4fColor:(SharedSkikoColor4f *)color colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("setColor4f(color:colorSpace:)")));
- (SharedSkikoPaint *)setStrokeValue:(BOOL)value __attribute__((swift_name("setStroke(value:)")));
@property int32_t alpha __attribute__((swift_name("alpha")));
@property (readonly) float alphaf __attribute__((swift_name("alphaf")));
@property SharedSkikoBlendMode *blendMode __attribute__((swift_name("blendMode")));
@property int32_t color __attribute__((swift_name("color")));
@property SharedSkikoColor4f *color4f __attribute__((swift_name("color4f")));
@property SharedSkikoColorFilter * _Nullable colorFilter __attribute__((swift_name("colorFilter")));
@property SharedSkikoImageFilter * _Nullable imageFilter __attribute__((swift_name("imageFilter")));
@property BOOL isAntiAlias __attribute__((swift_name("isAntiAlias")));
@property BOOL isDither __attribute__((swift_name("isDither")));
@property (readonly) BOOL isSrcOver __attribute__((swift_name("isSrcOver")));
@property SharedSkikoMaskFilter * _Nullable maskFilter __attribute__((swift_name("maskFilter")));
@property SharedSkikoPaintMode *mode __attribute__((swift_name("mode")));
@property SharedSkikoPathEffect * _Nullable pathEffect __attribute__((swift_name("pathEffect")));
@property SharedSkikoShader * _Nullable shader __attribute__((swift_name("shader")));
@property SharedSkikoPaintStrokeCap *strokeCap __attribute__((swift_name("strokeCap")));
@property SharedSkikoPaintStrokeJoin *strokeJoin __attribute__((swift_name("strokeJoin")));
@property float strokeMiter __attribute__((swift_name("strokeMiter")));
@property float strokeWidth __attribute__((swift_name("strokeWidth")));
@end


/**
 * @note annotations
 *   androidx.compose.runtime.Immutable
*/
__attribute__((swift_name("Ui_graphicsColorFilter")))
@interface SharedUi_graphicsColorFilter : SharedBase
@property (class, readonly, getter=companion) SharedUi_graphicsColorFilterCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((swift_name("Ui_graphicsPathEffect")))
@protocol SharedUi_graphicsPathEffect
@required
@end

__attribute__((swift_name("SkikoRefCnt")))
@interface SharedSkikoRefCnt : SharedSkikoManaged

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t refCount __attribute__((swift_name("refCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoShader")))
@interface SharedSkikoShader : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoShaderCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoShader *)makeWithColorFilterF:(SharedSkikoColorFilter * _Nullable)f __attribute__((swift_name("makeWithColorFilter(f:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeUtcOffset.Companion")))
@interface SharedKotlinx_datetimeUtcOffsetCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeUtcOffsetCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBlock:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset>))block __attribute__((swift_name("Format(block:)")));
- (SharedKotlinx_datetimeUtcOffset *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) SharedKotlinx_datetimeUtcOffset *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateProgression.Companion")))
@interface SharedKotlinx_datetimeLocalDateProgressionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateProgressionCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateRange.Companion")))
@interface SharedKotlinx_datetimeLocalDateRangeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateRangeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinx_datetimeLocalDateRange *EMPTY __attribute__((swift_name("EMPTY")));
@end

__attribute__((swift_name("KotlinAppendable")))
@protocol SharedKotlinAppendable
@required
- (id<SharedKotlinAppendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (id<SharedKotlinAppendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (id<SharedKotlinAppendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimePadding")))
@interface SharedKotlinx_datetimePadding : SharedKotlinEnum<SharedKotlinx_datetimePadding *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimePadding *none __attribute__((swift_name("none")));
@property (class, readonly) SharedKotlinx_datetimePadding *zero __attribute__((swift_name("zero")));
@property (class, readonly) SharedKotlinx_datetimePadding *space __attribute__((swift_name("space")));
+ (SharedKotlinArray<SharedKotlinx_datetimePadding *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimePadding *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeekNames")))
@interface SharedKotlinx_datetimeDayOfWeekNames : SharedBase
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMonday:(NSString *)monday tuesday:(NSString *)tuesday wednesday:(NSString *)wednesday thursday:(NSString *)thursday friday:(NSString *)friday saturday:(NSString *)saturday sunday:(NSString *)sunday __attribute__((swift_name("init(monday:tuesday:wednesday:thursday:friday:saturday:sunday:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeDayOfWeekNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonthNames")))
@interface SharedKotlinx_datetimeMonthNames : SharedBase
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithJanuary:(NSString *)january february:(NSString *)february march:(NSString *)march april:(NSString *)april may:(NSString *)may june:(NSString *)june july:(NSString *)july august:(NSString *)august september:(NSString *)september october:(NSString *)october november:(NSString *)november december:(NSString *)december __attribute__((swift_name("init(january:february:march:april:may:june:july:august:september:october:november:december:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeMonthNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontSmoothing")))
@interface SharedUi_textFontSmoothing : SharedKotlinEnum<SharedUi_textFontSmoothing *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedUi_textFontSmoothing *none __attribute__((swift_name("none")));
@property (class, readonly) SharedUi_textFontSmoothing *antialias __attribute__((swift_name("antialias")));
@property (class, readonly) SharedUi_textFontSmoothing *subpixelantialias __attribute__((swift_name("subpixelantialias")));
+ (SharedKotlinArray<SharedUi_textFontSmoothing *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedUi_textFontSmoothing *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontHinting")))
@interface SharedUi_textFontHinting : SharedKotlinEnum<SharedUi_textFontHinting *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedUi_textFontHinting *none __attribute__((swift_name("none")));
@property (class, readonly) SharedUi_textFontHinting *slight __attribute__((swift_name("slight")));
@property (class, readonly) SharedUi_textFontHinting *normal __attribute__((swift_name("normal")));
@property (class, readonly) SharedUi_textFontHinting *full __attribute__((swift_name("full")));
+ (SharedKotlinArray<SharedUi_textFontHinting *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedUi_textFontHinting *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_textFontRasterizationSettings.Companion")))
@interface SharedUi_textFontRasterizationSettingsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_textFontRasterizationSettingsCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedUi_textFontRasterizationSettings *PlatformDefault __attribute__((swift_name("PlatformDefault")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoNative.Companion")))
@interface SharedSkikoNativeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoNativeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) void * _Nullable NullPointer __attribute__((swift_name("NullPointer")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPaint.Companion")))
@interface SharedSkikoPaintCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPaintCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColor4f")))
@interface SharedSkikoColor4f : SharedBase
- (instancetype)initWithRgba:(SharedKotlinFloatArray *)rgba __attribute__((swift_name("init(rgba:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithC:(int32_t)c __attribute__((swift_name("init(c:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithR:(float)r g:(float)g b:(float)b a:(float)a __attribute__((swift_name("init(r:g:b:a:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoColor4fCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedKotlinFloatArray *)flatten __attribute__((swift_name("flatten()")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoColor4f *)makeLerpOther:(SharedSkikoColor4f *)other weight:(float)weight __attribute__((swift_name("makeLerp(other:weight:)")));
- (int32_t)toColor __attribute__((swift_name("toColor()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoColor4f *)withA_a:(float)_a __attribute__((swift_name("withA(_a:)")));
- (SharedSkikoColor4f *)withB_b:(float)_b __attribute__((swift_name("withB(_b:)")));
- (SharedSkikoColor4f *)withG_g:(float)_g __attribute__((swift_name("withG(_g:)")));
- (SharedSkikoColor4f *)withR_r:(float)_r __attribute__((swift_name("withR(_r:)")));
@property (readonly) float a __attribute__((swift_name("a")));
@property (readonly) float b __attribute__((swift_name("b")));
@property (readonly) float g __attribute__((swift_name("g")));
@property (readonly) float r __attribute__((swift_name("r")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorSpace")))
@interface SharedSkikoColorSpace : SharedSkikoManaged
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoColorSpaceCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoColor4f *)convertToColor:(SharedSkikoColorSpace * _Nullable)toColor color:(SharedSkikoColor4f *)color __attribute__((swift_name("convert(toColor:color:)")));
@property (readonly) BOOL isGammaCloseToSRGB __attribute__((swift_name("isGammaCloseToSRGB")));
@property (readonly) BOOL isGammaLinear __attribute__((swift_name("isGammaLinear")));
@property (readonly) BOOL isSRGB __attribute__((swift_name("isSRGB")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoBlendMode")))
@interface SharedSkikoBlendMode : SharedKotlinEnum<SharedSkikoBlendMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoBlendMode *clear __attribute__((swift_name("clear")));
@property (class, readonly) SharedSkikoBlendMode *src __attribute__((swift_name("src")));
@property (class, readonly) SharedSkikoBlendMode *dst __attribute__((swift_name("dst")));
@property (class, readonly) SharedSkikoBlendMode *srcOver __attribute__((swift_name("srcOver")));
@property (class, readonly) SharedSkikoBlendMode *dstOver __attribute__((swift_name("dstOver")));
@property (class, readonly) SharedSkikoBlendMode *srcIn __attribute__((swift_name("srcIn")));
@property (class, readonly) SharedSkikoBlendMode *dstIn __attribute__((swift_name("dstIn")));
@property (class, readonly) SharedSkikoBlendMode *srcOut __attribute__((swift_name("srcOut")));
@property (class, readonly) SharedSkikoBlendMode *dstOut __attribute__((swift_name("dstOut")));
@property (class, readonly) SharedSkikoBlendMode *srcAtop __attribute__((swift_name("srcAtop")));
@property (class, readonly) SharedSkikoBlendMode *dstAtop __attribute__((swift_name("dstAtop")));
@property (class, readonly) SharedSkikoBlendMode *xor_ __attribute__((swift_name("xor_")));
@property (class, readonly) SharedSkikoBlendMode *plus __attribute__((swift_name("plus")));
@property (class, readonly) SharedSkikoBlendMode *modulate __attribute__((swift_name("modulate")));
@property (class, readonly) SharedSkikoBlendMode *screen __attribute__((swift_name("screen")));
@property (class, readonly) SharedSkikoBlendMode *overlay __attribute__((swift_name("overlay")));
@property (class, readonly) SharedSkikoBlendMode *darken __attribute__((swift_name("darken")));
@property (class, readonly) SharedSkikoBlendMode *lighten __attribute__((swift_name("lighten")));
@property (class, readonly) SharedSkikoBlendMode *colorDodge __attribute__((swift_name("colorDodge")));
@property (class, readonly) SharedSkikoBlendMode *colorBurn __attribute__((swift_name("colorBurn")));
@property (class, readonly) SharedSkikoBlendMode *hardLight __attribute__((swift_name("hardLight")));
@property (class, readonly) SharedSkikoBlendMode *softLight __attribute__((swift_name("softLight")));
@property (class, readonly) SharedSkikoBlendMode *difference __attribute__((swift_name("difference")));
@property (class, readonly) SharedSkikoBlendMode *exclusion __attribute__((swift_name("exclusion")));
@property (class, readonly) SharedSkikoBlendMode *multiply __attribute__((swift_name("multiply")));
@property (class, readonly) SharedSkikoBlendMode *hue __attribute__((swift_name("hue")));
@property (class, readonly) SharedSkikoBlendMode *saturation __attribute__((swift_name("saturation")));
@property (class, readonly) SharedSkikoBlendMode *color __attribute__((swift_name("color")));
@property (class, readonly) SharedSkikoBlendMode *luminosity __attribute__((swift_name("luminosity")));
+ (SharedKotlinArray<SharedSkikoBlendMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoBlendMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorFilter")))
@interface SharedSkikoColorFilter : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoColorFilterCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImageFilter")))
@interface SharedSkikoImageFilter : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoImageFilterCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMaskFilter")))
@interface SharedSkikoMaskFilter : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoMaskFilterCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPaintMode")))
@interface SharedSkikoPaintMode : SharedKotlinEnum<SharedSkikoPaintMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPaintMode *fill __attribute__((swift_name("fill")));
@property (class, readonly) SharedSkikoPaintMode *stroke __attribute__((swift_name("stroke")));
@property (class, readonly) SharedSkikoPaintMode *strokeAndFill __attribute__((swift_name("strokeAndFill")));
+ (SharedKotlinArray<SharedSkikoPaintMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPaintMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathEffect")))
@interface SharedSkikoPathEffect : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPathEffectCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoPathEffect *)makeComposeInner:(SharedSkikoPathEffect * _Nullable)inner __attribute__((swift_name("makeCompose(inner:)")));
- (SharedSkikoPathEffect *)makeSumSecond:(SharedSkikoPathEffect * _Nullable)second __attribute__((swift_name("makeSum(second:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPaintStrokeCap")))
@interface SharedSkikoPaintStrokeCap : SharedKotlinEnum<SharedSkikoPaintStrokeCap *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPaintStrokeCap *butt __attribute__((swift_name("butt")));
@property (class, readonly) SharedSkikoPaintStrokeCap *round __attribute__((swift_name("round")));
@property (class, readonly) SharedSkikoPaintStrokeCap *square __attribute__((swift_name("square")));
+ (SharedKotlinArray<SharedSkikoPaintStrokeCap *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPaintStrokeCap *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPaintStrokeJoin")))
@interface SharedSkikoPaintStrokeJoin : SharedKotlinEnum<SharedSkikoPaintStrokeJoin *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPaintStrokeJoin *miter __attribute__((swift_name("miter")));
@property (class, readonly) SharedSkikoPaintStrokeJoin *round __attribute__((swift_name("round")));
@property (class, readonly) SharedSkikoPaintStrokeJoin *bevel __attribute__((swift_name("bevel")));
+ (SharedKotlinArray<SharedSkikoPaintStrokeJoin *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPaintStrokeJoin *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ui_graphicsColorFilter.Companion")))
@interface SharedUi_graphicsColorFilterCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUi_graphicsColorFilterCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsColorFilter *)colorMatrixColorMatrix:(id)colorMatrix __attribute__((swift_name("colorMatrix(colorMatrix:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsColorFilter *)lightingMultiply:(uint64_t)multiply add:(uint64_t)add __attribute__((swift_name("lighting(multiply:add:)")));

/**
 * @note annotations
 *   androidx.compose.runtime.Stable
*/
- (SharedUi_graphicsColorFilter *)tintColor:(uint64_t)color blendMode:(int32_t)blendMode __attribute__((swift_name("tint(color:blendMode:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoShader.Companion")))
@interface SharedSkikoShaderCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoShaderCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoShader *)makeBlendMode:(SharedSkikoBlendMode *)mode dst:(SharedSkikoShader * _Nullable)dst src:(SharedSkikoShader * _Nullable)src __attribute__((swift_name("makeBlend(mode:dst:src:)")));
- (SharedSkikoShader *)makeColorColor:(int32_t)color __attribute__((swift_name("makeColor(color:)")));
- (SharedSkikoShader *)makeColorColor:(SharedSkikoColor4f *)color space:(SharedSkikoColorSpace * _Nullable)space __attribute__((swift_name("makeColor(color:space:)")));
- (SharedSkikoShader *)makeEmpty __attribute__((swift_name("makeEmpty()")));
- (SharedSkikoShader *)makeFractalNoiseBaseFrequencyX:(float)baseFrequencyX baseFrequencyY:(float)baseFrequencyY numOctaves:(int32_t)numOctaves seed:(float)seed tileSize:(SharedSkikoISize *)tileSize __attribute__((swift_name("makeFractalNoise(baseFrequencyX:baseFrequencyY:numOctaves:seed:tileSize:)")));
- (SharedSkikoShader *)makeLinearGradientP0:(SharedSkikoPoint *)p0 p1:(SharedSkikoPoint *)p1 colors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeLinearGradient(p0:p1:colors:)")));
- (SharedSkikoShader *)makeLinearGradientP0:(SharedSkikoPoint *)p0 p1:(SharedSkikoPoint *)p1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions __attribute__((swift_name("makeLinearGradient(p0:p1:colors:positions:)")));
- (SharedSkikoShader *)makeLinearGradientP0:(SharedSkikoPoint *)p0 p1:(SharedSkikoPoint *)p1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeLinearGradient(p0:p1:colors:positions:style:)")));
- (SharedSkikoShader *)makeLinearGradientP0:(SharedSkikoPoint *)p0 p1:(SharedSkikoPoint *)p1 colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeLinearGradient(p0:p1:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeLinearGradientX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeLinearGradient(x0:y0:x1:y1:colors:positions:style:)")));
- (SharedSkikoShader *)makeLinearGradientX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeLinearGradient(x0:y0:x1:y1:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeRadialGradientCenter:(SharedSkikoPoint *)center r:(float)r colors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeRadialGradient(center:r:colors:)")));
- (SharedSkikoShader *)makeRadialGradientCenter:(SharedSkikoPoint *)center r:(float)r colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions __attribute__((swift_name("makeRadialGradient(center:r:colors:positions:)")));
- (SharedSkikoShader *)makeRadialGradientCenter:(SharedSkikoPoint *)center r:(float)r colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeRadialGradient(center:r:colors:positions:style:)")));
- (SharedSkikoShader *)makeRadialGradientX:(float)x y:(float)y r:(float)r colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeRadialGradient(x:y:r:colors:positions:style:)")));
- (SharedSkikoShader *)makeRadialGradientCenter:(SharedSkikoPoint *)center r:(float)r colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeRadialGradient(center:r:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeRadialGradientX:(float)x y:(float)y r:(float)r colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeRadialGradient(x:y:r:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientCenter:(SharedSkikoPoint *)center colors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeSweepGradient(center:colors:)")));
- (SharedSkikoShader *)makeSweepGradientX:(float)x y:(float)y colors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeSweepGradient(x:y:colors:)")));
- (SharedSkikoShader *)makeSweepGradientCenter:(SharedSkikoPoint *)center colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions __attribute__((swift_name("makeSweepGradient(center:colors:positions:)")));
- (SharedSkikoShader *)makeSweepGradientX:(float)x y:(float)y colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions __attribute__((swift_name("makeSweepGradient(x:y:colors:positions:)")));
- (SharedSkikoShader *)makeSweepGradientCenter:(SharedSkikoPoint *)center colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(center:colors:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientX:(float)x y:(float)y colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(x:y:colors:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientCenter:(SharedSkikoPoint *)center startAngle:(float)startAngle endAngle:(float)endAngle colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(center:startAngle:endAngle:colors:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientX:(float)x y:(float)y startAngle:(float)startAngle endAngle:(float)endAngle colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(x:y:startAngle:endAngle:colors:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientCenter:(SharedSkikoPoint *)center startAngle:(float)startAngle endAngle:(float)endAngle colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(center:startAngle:endAngle:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeSweepGradientX:(float)x y:(float)y startAngle:(float)startAngle endAngle:(float)endAngle colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeSweepGradient(x:y:startAngle:endAngle:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeTurbulenceBaseFrequencyX:(float)baseFrequencyX baseFrequencyY:(float)baseFrequencyY numOctaves:(int32_t)numOctaves seed:(float)seed tileSize:(SharedSkikoISize *)tileSize __attribute__((swift_name("makeTurbulence(baseFrequencyX:baseFrequencyY:numOctaves:seed:tileSize:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientP0:(SharedSkikoPoint *)p0 r0:(float)r0 p1:(SharedSkikoPoint *)p1 r1:(float)r1 colors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeTwoPointConicalGradient(p0:r0:p1:r1:colors:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientP0:(SharedSkikoPoint *)p0 r0:(float)r0 p1:(SharedSkikoPoint *)p1 r1:(float)r1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions __attribute__((swift_name("makeTwoPointConicalGradient(p0:r0:p1:r1:colors:positions:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientP0:(SharedSkikoPoint *)p0 r0:(float)r0 p1:(SharedSkikoPoint *)p1 r1:(float)r1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeTwoPointConicalGradient(p0:r0:p1:r1:colors:positions:style:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientP0:(SharedSkikoPoint *)p0 r0:(float)r0 p1:(SharedSkikoPoint *)p1 r1:(float)r1 colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeTwoPointConicalGradient(p0:r0:p1:r1:colors:cs:positions:style:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientX0:(float)x0 y0:(float)y0 r0:(float)r0 x1:(float)x1 y1:(float)y1 r1:(float)r1 colors:(SharedKotlinIntArray *)colors positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeTwoPointConicalGradient(x0:y0:r0:x1:y1:r1:colors:positions:style:)")));
- (SharedSkikoShader *)makeTwoPointConicalGradientX0:(float)x0 y0:(float)y0 r0:(float)r0 x1:(float)x1 y1:(float)y1 r1:(float)r1 colors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors cs:(SharedSkikoColorSpace * _Nullable)cs positions:(SharedKotlinFloatArray * _Nullable)positions style:(SharedSkikoGradientStyle *)style __attribute__((swift_name("makeTwoPointConicalGradient(x0:y0:r0:x1:y1:r1:colors:cs:positions:style:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithUtcOffset")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)offsetFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("offset(format:)")));
- (void)offsetHoursPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetHours(padding:)")));
- (void)offsetMinutesOfHourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetMinutesOfHour(padding:)")));
- (void)offsetSecondsOfMinutePadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetSecondsOfMinute(padding:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeekNames.Companion")))
@interface SharedKotlinx_datetimeDayOfWeekNamesCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeDayOfWeekNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinx_datetimeDayOfWeekNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) SharedKotlinx_datetimeDayOfWeekNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonthNames.Companion")))
@interface SharedKotlinx_datetimeMonthNamesCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeMonthNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinx_datetimeMonthNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) SharedKotlinx_datetimeMonthNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinFloatArray")))
@interface SharedKotlinFloatArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedFloat *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (float)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinFloatIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(float)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColor4f.Companion")))
@interface SharedSkikoColor4fCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoColor4fCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinFloatArray *)flattenArrayColors:(SharedKotlinArray<SharedSkikoColor4f *> *)colors __attribute__((swift_name("flattenArray(colors:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorSpace.Companion")))
@interface SharedSkikoColorSpaceCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoColorSpaceCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoColorSpace *displayP3 __attribute__((swift_name("displayP3")));
@property (readonly) SharedSkikoColorSpace *sRGB __attribute__((swift_name("sRGB")));
@property (readonly) SharedSkikoColorSpace *sRGBLinear __attribute__((swift_name("sRGBLinear")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorFilter.Companion")))
@interface SharedSkikoColorFilterCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoColorFilterCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoColorFilter *)makeBlendColor:(int32_t)color mode:(SharedSkikoBlendMode *)mode __attribute__((swift_name("makeBlend(color:mode:)")));
- (SharedSkikoColorFilter *)makeComposedOuter:(SharedSkikoColorFilter * _Nullable)outer inner:(SharedSkikoColorFilter * _Nullable)inner __attribute__((swift_name("makeComposed(outer:inner:)")));
- (SharedSkikoColorFilter *)makeHSLAMatrixMatrix:(SharedSkikoColorMatrix *)matrix __attribute__((swift_name("makeHSLAMatrix(matrix:)")));
- (SharedSkikoColorFilter *)makeHighContrastGrayscale:(BOOL)grayscale mode:(SharedSkikoInversionMode *)mode contrast:(float)contrast __attribute__((swift_name("makeHighContrast(grayscale:mode:contrast:)")));
- (SharedSkikoColorFilter *)makeLerpDst:(SharedSkikoColorFilter * _Nullable)dst src:(SharedSkikoColorFilter * _Nullable)src t:(float)t __attribute__((swift_name("makeLerp(dst:src:t:)")));
- (SharedSkikoColorFilter *)makeLightingColorMul:(int32_t)colorMul colorAdd:(int32_t)colorAdd __attribute__((swift_name("makeLighting(colorMul:colorAdd:)")));
- (SharedSkikoColorFilter *)makeMatrixMatrix:(SharedSkikoColorMatrix *)matrix __attribute__((swift_name("makeMatrix(matrix:)")));
- (SharedSkikoColorFilter *)makeOverdrawColors:(SharedKotlinIntArray *)colors __attribute__((swift_name("makeOverdraw(colors:)")));
- (SharedSkikoColorFilter *)makeTableTable:(SharedKotlinByteArray *)table __attribute__((swift_name("makeTable(table:)")));
- (SharedSkikoColorFilter *)makeTableARGBA:(SharedKotlinByteArray * _Nullable)a r:(SharedKotlinByteArray * _Nullable)r g:(SharedKotlinByteArray * _Nullable)g b:(SharedKotlinByteArray * _Nullable)b __attribute__((swift_name("makeTableARGB(a:r:g:b:)")));
@property (readonly) SharedSkikoColorFilter *luma __attribute__((swift_name("luma")));
@property (readonly) SharedSkikoColorFilter *sRGBToLinearGamma __attribute__((swift_name("sRGBToLinearGamma")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImageFilter.Companion")))
@interface SharedSkikoImageFilterCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoImageFilterCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoImageFilter *)makeArithmeticK1:(float)k1 k2:(float)k2 k3:(float)k3 k4:(float)k4 enforcePMColor:(BOOL)enforcePMColor bg:(SharedSkikoImageFilter * _Nullable)bg fg:(SharedSkikoImageFilter * _Nullable)fg crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeArithmetic(k1:k2:k3:k4:enforcePMColor:bg:fg:crop:)")));
- (SharedSkikoImageFilter *)makeBlendBlendMode:(SharedSkikoBlendMode *)blendMode bg:(SharedSkikoImageFilter * _Nullable)bg fg:(SharedSkikoImageFilter * _Nullable)fg crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeBlend(blendMode:bg:fg:crop:)")));
- (SharedSkikoImageFilter *)makeBlurSigmaX:(float)sigmaX sigmaY:(float)sigmaY mode:(SharedSkikoFilterTileMode *)mode input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeBlur(sigmaX:sigmaY:mode:input:crop:)")));
- (SharedSkikoImageFilter *)makeColorFilterF:(SharedSkikoColorFilter * _Nullable)f input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeColorFilter(f:input:crop:)")));
- (SharedSkikoImageFilter *)makeComposeOuter:(SharedSkikoImageFilter * _Nullable)outer inner:(SharedSkikoImageFilter * _Nullable)inner __attribute__((swift_name("makeCompose(outer:inner:)")));
- (SharedSkikoImageFilter *)makeDilateRx:(float)rx ry:(float)ry input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDilate(rx:ry:input:crop:)")));
- (SharedSkikoImageFilter *)makeDisplacementMapX:(SharedSkikoColorChannel *)x y:(SharedSkikoColorChannel *)y scale:(float)scale displacement:(SharedSkikoImageFilter * _Nullable)displacement color:(SharedSkikoImageFilter * _Nullable)color crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDisplacementMap(x:y:scale:displacement:color:crop:)")));
- (SharedSkikoImageFilter *)makeDistantLitDiffuseX:(float)x y:(float)y z:(float)z lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale kd:(float)kd input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDistantLitDiffuse(x:y:z:lightColor:surfaceScale:kd:input:crop:)")));
- (SharedSkikoImageFilter *)makeDistantLitSpecularX:(float)x y:(float)y z:(float)z lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale ks:(float)ks shininess:(float)shininess input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDistantLitSpecular(x:y:z:lightColor:surfaceScale:ks:shininess:input:crop:)")));
- (SharedSkikoImageFilter *)makeDropShadowDx:(float)dx dy:(float)dy sigmaX:(float)sigmaX sigmaY:(float)sigmaY color:(int32_t)color input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDropShadow(dx:dy:sigmaX:sigmaY:color:input:crop:)")));
- (SharedSkikoImageFilter *)makeDropShadowOnlyDx:(float)dx dy:(float)dy sigmaX:(float)sigmaX sigmaY:(float)sigmaY color:(int32_t)color input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeDropShadowOnly(dx:dy:sigmaX:sigmaY:color:input:crop:)")));
- (SharedSkikoImageFilter *)makeErodeRx:(float)rx ry:(float)ry input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeErode(rx:ry:input:crop:)")));
- (SharedSkikoImageFilter *)makeImageImage:(SharedSkikoImage *)image __attribute__((swift_name("makeImage(image:)")));
- (SharedSkikoImageFilter *)makeImageImage:(SharedSkikoImage * _Nullable)image src:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst mode:(id<SharedSkikoSamplingMode>)mode __attribute__((swift_name("makeImage(image:src:dst:mode:)")));
- (SharedSkikoImageFilter *)makeMagnifierR:(SharedSkikoRect *)r zoomAmount:(float)zoomAmount inset:(float)inset samplingMode:(id<SharedSkikoSamplingMode>)samplingMode input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeMagnifier(r:zoomAmount:inset:samplingMode:input:crop:)")));
- (SharedSkikoImageFilter *)makeMatrixConvolutionKernelW:(int32_t)kernelW kernelH:(int32_t)kernelH kernel:(SharedKotlinFloatArray * _Nullable)kernel gain:(float)gain bias:(float)bias offsetX:(int32_t)offsetX offsetY:(int32_t)offsetY tileMode:(SharedSkikoFilterTileMode *)tileMode convolveAlpha:(BOOL)convolveAlpha input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeMatrixConvolution(kernelW:kernelH:kernel:gain:bias:offsetX:offsetY:tileMode:convolveAlpha:input:crop:)")));
- (SharedSkikoImageFilter *)makeMatrixTransformMatrix:(SharedSkikoMatrix33 *)matrix mode:(id<SharedSkikoSamplingMode>)mode input:(SharedSkikoImageFilter * _Nullable)input __attribute__((swift_name("makeMatrixTransform(matrix:mode:input:)")));
- (SharedSkikoImageFilter *)makeMergeFilters:(SharedKotlinArray<SharedSkikoImageFilter *> *)filters crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeMerge(filters:crop:)")));
- (SharedSkikoImageFilter *)makeOffsetDx:(float)dx dy:(float)dy input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeOffset(dx:dy:input:crop:)")));
- (SharedSkikoImageFilter *)makePointLitDiffuseX:(float)x y:(float)y z:(float)z lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale kd:(float)kd input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makePointLitDiffuse(x:y:z:lightColor:surfaceScale:kd:input:crop:)")));
- (SharedSkikoImageFilter *)makePointLitSpecularX:(float)x y:(float)y z:(float)z lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale ks:(float)ks shininess:(float)shininess input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makePointLitSpecular(x:y:z:lightColor:surfaceScale:ks:shininess:input:crop:)")));
- (SharedSkikoImageFilter *)makeRuntimeShaderRuntimeShaderBuilder:(SharedSkikoRuntimeShaderBuilder *)runtimeShaderBuilder shaderNames:(SharedKotlinArray<NSString *> *)shaderNames inputs:(SharedKotlinArray<SharedSkikoImageFilter *> *)inputs __attribute__((swift_name("makeRuntimeShader(runtimeShaderBuilder:shaderNames:inputs:)")));
- (SharedSkikoImageFilter *)makeRuntimeShaderRuntimeShaderBuilder:(SharedSkikoRuntimeShaderBuilder *)runtimeShaderBuilder shaderName:(NSString *)shaderName input:(SharedSkikoImageFilter * _Nullable)input __attribute__((swift_name("makeRuntimeShader(runtimeShaderBuilder:shaderName:input:)")));
- (SharedSkikoImageFilter *)makeShaderShader:(SharedSkikoShader *)shader dither:(BOOL)dither crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeShader(shader:dither:crop:)")));
- (SharedSkikoImageFilter *)makeSpotLitDiffuseX0:(float)x0 y0:(float)y0 z0:(float)z0 x1:(float)x1 y1:(float)y1 z1:(float)z1 falloffExponent:(float)falloffExponent cutoffAngle:(float)cutoffAngle lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale kd:(float)kd input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeSpotLitDiffuse(x0:y0:z0:x1:y1:z1:falloffExponent:cutoffAngle:lightColor:surfaceScale:kd:input:crop:)")));
- (SharedSkikoImageFilter *)makeSpotLitSpecularX0:(float)x0 y0:(float)y0 z0:(float)z0 x1:(float)x1 y1:(float)y1 z1:(float)z1 falloffExponent:(float)falloffExponent cutoffAngle:(float)cutoffAngle lightColor:(int32_t)lightColor surfaceScale:(float)surfaceScale ks:(float)ks shininess:(float)shininess input:(SharedSkikoImageFilter * _Nullable)input crop:(SharedSkikoIRect * _Nullable)crop __attribute__((swift_name("makeSpotLitSpecular(x0:y0:z0:x1:y1:z1:falloffExponent:cutoffAngle:lightColor:surfaceScale:ks:shininess:input:crop:)")));
- (SharedSkikoImageFilter *)makeTileSrc:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst input:(SharedSkikoImageFilter * _Nullable)input __attribute__((swift_name("makeTile(src:dst:input:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMaskFilter.Companion")))
@interface SharedSkikoMaskFilterCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoMaskFilterCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoMaskFilter *)makeBlurMode:(SharedSkikoFilterBlurMode *)mode sigma:(float)sigma respectCTM:(BOOL)respectCTM __attribute__((swift_name("makeBlur(mode:sigma:respectCTM:)")));
- (SharedSkikoMaskFilter *)makeClipMin:(int32_t)min max:(int32_t)max __attribute__((swift_name("makeClip(min:max:)")));
- (SharedSkikoMaskFilter *)makeGammaGamma:(float)gamma __attribute__((swift_name("makeGamma(gamma:)")));
- (SharedSkikoMaskFilter *)makeShaderS:(SharedSkikoShader * _Nullable)s __attribute__((swift_name("makeShader(s:)")));
- (SharedSkikoMaskFilter *)makeTableTable:(SharedKotlinByteArray *)table __attribute__((swift_name("makeTable(table:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathEffect.Companion")))
@interface SharedSkikoPathEffectCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPathEffectCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoPathEffect *)makeCornerRadius:(float)radius __attribute__((swift_name("makeCorner(radius:)")));
- (SharedSkikoPathEffect *)makeDashIntervals:(SharedKotlinFloatArray *)intervals phase:(float)phase __attribute__((swift_name("makeDash(intervals:phase:)")));
- (SharedSkikoPathEffect *)makeDiscreteSegLength:(float)segLength dev:(float)dev seed:(int32_t)seed __attribute__((swift_name("makeDiscrete(segLength:dev:seed:)")));
- (SharedSkikoPathEffect *)makeLine2DWidth:(float)width matrix:(SharedSkikoMatrix33 *)matrix __attribute__((swift_name("makeLine2D(width:matrix:)")));
- (SharedSkikoPathEffect *)makePath1DPath:(SharedSkikoPath *)path advance:(float)advance phase:(float)phase style:(SharedSkikoPathEffectStyle *)style __attribute__((swift_name("makePath1D(path:advance:phase:style:)")));
- (SharedSkikoPathEffect *)makePath2DMatrix:(SharedSkikoMatrix33 *)matrix path:(SharedSkikoPath *)path __attribute__((swift_name("makePath2D(matrix:path:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoISize")))
@interface SharedSkikoISize : SharedBase
@property (class, readonly, getter=companion) SharedSkikoISizeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)area __attribute__((swift_name("area()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (BOOL)isZero __attribute__((swift_name("isZero()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPoint")))
@interface SharedSkikoPoint : SharedBase
- (instancetype)initWithX:(float)x y:(float)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoPointCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoPoint *)offsetVec:(SharedSkikoPoint *)vec __attribute__((swift_name("offset(vec:)")));
- (SharedSkikoPoint *)offsetDx:(float)dx dy:(float)dy __attribute__((swift_name("offset(dx:dy:)")));
- (SharedSkikoPoint *)scaleScale:(float)scale __attribute__((swift_name("scale(scale:)")));
- (SharedSkikoPoint *)scaleSx:(float)sx sy:(float)sy __attribute__((swift_name("scale(sx:sy:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) float x __attribute__((swift_name("x")));
@property (readonly) float y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntArray")))
@interface SharedKotlinIntArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedInt *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int32_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int32_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoGradientStyle")))
@interface SharedSkikoGradientStyle : SharedBase
- (instancetype)initWithTileMode:(SharedSkikoFilterTileMode *)tileMode isPremul:(BOOL)isPremul localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("init(tileMode:isPremul:localMatrix:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoGradientStyleCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoGradientStyle *)withLocalMatrix_localMatrix:(SharedSkikoMatrix33 *)_localMatrix __attribute__((swift_name("withLocalMatrix(_localMatrix:)")));
- (SharedSkikoGradientStyle *)withPremul_premul:(BOOL)_premul __attribute__((swift_name("withPremul(_premul:)")));
- (SharedSkikoGradientStyle *)withTileMode_tileMode:(SharedSkikoFilterTileMode *)_tileMode __attribute__((swift_name("withTileMode(_tileMode:)")));
@property (readonly) BOOL isPremul __attribute__((swift_name("isPremul")));
@property (readonly) SharedSkikoMatrix33 * _Nullable localMatrix __attribute__((swift_name("localMatrix")));
@property (readonly) SharedSkikoFilterTileMode *tileMode __attribute__((swift_name("tileMode")));
@end

__attribute__((swift_name("KotlinFloatIterator")))
@interface SharedKotlinFloatIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedFloat *)next __attribute__((swift_name("next()")));
- (float)nextFloat __attribute__((swift_name("nextFloat()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorMatrix")))
@interface SharedSkikoColorMatrix : SharedBase
- (instancetype)initWithMat:(SharedKotlinFloatArray *)mat __attribute__((swift_name("init(mat:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinFloatArray *mat __attribute__((swift_name("mat")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoInversionMode")))
@interface SharedSkikoInversionMode : SharedKotlinEnum<SharedSkikoInversionMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoInversionMode *no __attribute__((swift_name("no")));
@property (class, readonly) SharedSkikoInversionMode *brightness __attribute__((swift_name("brightness")));
@property (class, readonly) SharedSkikoInversionMode *lightness __attribute__((swift_name("lightness")));
+ (SharedKotlinArray<SharedSkikoInversionMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoInversionMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoIRect")))
@interface SharedSkikoIRect : SharedBase
@property (class, readonly, getter=companion) SharedSkikoIRectCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoIRect * _Nullable)intersectOther:(SharedSkikoIRect *)other __attribute__((swift_name("intersect(other:)")));
- (SharedSkikoIRect *)offsetVec:(SharedSkikoIPoint *)vec __attribute__((swift_name("offset(vec:)")));
- (SharedSkikoIRect *)offsetDx:(int32_t)dx dy:(int32_t)dy __attribute__((swift_name("offset(dx:dy:)")));
- (SharedSkikoRect *)toRect __attribute__((swift_name("toRect()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t bottom __attribute__((swift_name("bottom")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) int32_t left __attribute__((swift_name("left")));
@property (readonly) int32_t right __attribute__((swift_name("right")));
@property (readonly) int32_t top __attribute__((swift_name("top")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFilterTileMode")))
@interface SharedSkikoFilterTileMode : SharedKotlinEnum<SharedSkikoFilterTileMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFilterTileMode *clamp __attribute__((swift_name("clamp")));
@property (class, readonly) SharedSkikoFilterTileMode *repeat __attribute__((swift_name("repeat")));
@property (class, readonly) SharedSkikoFilterTileMode *mirror __attribute__((swift_name("mirror")));
@property (class, readonly) SharedSkikoFilterTileMode *decal __attribute__((swift_name("decal")));
+ (SharedKotlinArray<SharedSkikoFilterTileMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFilterTileMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorChannel")))
@interface SharedSkikoColorChannel : SharedKotlinEnum<SharedSkikoColorChannel *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoColorChannel *r __attribute__((swift_name("r")));
@property (class, readonly) SharedSkikoColorChannel *g __attribute__((swift_name("g")));
@property (class, readonly) SharedSkikoColorChannel *b __attribute__((swift_name("b")));
@property (class, readonly) SharedSkikoColorChannel *a __attribute__((swift_name("a")));
+ (SharedKotlinArray<SharedSkikoColorChannel *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoColorChannel *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("SkikoIHasImageInfo")))
@protocol SharedSkikoIHasImageInfo
@required
@property (readonly) SharedSkikoColorAlphaType *alphaType __attribute__((swift_name("alphaType")));
@property (readonly) int32_t bytesPerPixel __attribute__((swift_name("bytesPerPixel")));
@property (readonly) SharedSkikoColorInfo *colorInfo __attribute__((swift_name("colorInfo")));
@property (readonly) SharedSkikoColorSpace * _Nullable colorSpace __attribute__((swift_name("colorSpace")));
@property (readonly) SharedSkikoColorType *colorType __attribute__((swift_name("colorType")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) SharedSkikoImageInfo *imageInfo __attribute__((swift_name("imageInfo")));
@property (readonly, getter=isEmpty_) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isOpaque __attribute__((swift_name("isOpaque")));
@property (readonly) int32_t shiftPerPixel __attribute__((swift_name("shiftPerPixel")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImage")))
@interface SharedSkikoImage : SharedSkikoRefCnt <SharedSkikoIHasImageInfo>

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoImageCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoData * _Nullable)encodeToDataFormat:(SharedSkikoEncodedImageFormat *)format quality:(int32_t)quality __attribute__((swift_name("encodeToData(format:quality:)")));
- (SharedSkikoShader *)makeShaderLocalMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(localMatrix:)")));
- (SharedSkikoShader *)makeShaderTmx:(SharedSkikoFilterTileMode *)tmx tmy:(SharedSkikoFilterTileMode *)tmy localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(tmx:tmy:localMatrix:)")));
- (SharedSkikoShader *)makeShaderTmx:(SharedSkikoFilterTileMode *)tmx tmy:(SharedSkikoFilterTileMode *)tmy sampling:(id<SharedSkikoSamplingMode>)sampling localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(tmx:tmy:sampling:localMatrix:)")));
- (SharedSkikoPixmap * _Nullable)peekPixels __attribute__((swift_name("peekPixels()")));
- (BOOL)peekPixelsPixmap:(SharedSkikoPixmap * _Nullable)pixmap __attribute__((swift_name("peekPixels(pixmap:)")));
- (BOOL)readPixelsDst:(SharedSkikoBitmap *)dst __attribute__((swift_name("readPixels(dst:)")));
- (BOOL)readPixelsContext:(SharedSkikoDirectContext *)context dst:(SharedSkikoBitmap *)dst __attribute__((swift_name("readPixels(context:dst:)")));
- (BOOL)readPixelsDst:(SharedSkikoBitmap *)dst srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(dst:srcX:srcY:)")));
- (BOOL)readPixelsContext:(SharedSkikoDirectContext *)context dst:(SharedSkikoBitmap *)dst srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(context:dst:srcX:srcY:)")));
- (BOOL)readPixelsDst:(SharedSkikoPixmap *)dst srcX:(int32_t)srcX srcY:(int32_t)srcY cache:(BOOL)cache __attribute__((swift_name("readPixels(dst:srcX:srcY:cache:)")));
- (BOOL)readPixelsContext:(SharedSkikoDirectContext * _Nullable)context dst:(SharedSkikoBitmap *)dst srcX:(int32_t)srcX srcY:(int32_t)srcY cache:(BOOL)cache __attribute__((swift_name("readPixels(context:dst:srcX:srcY:cache:)")));
- (BOOL)scalePixelsDst:(SharedSkikoPixmap *)dst samplingMode:(id<SharedSkikoSamplingMode>)samplingMode cache:(BOOL)cache __attribute__((swift_name("scalePixels(dst:samplingMode:cache:)")));
@property (readonly) SharedSkikoImageInfo *imageInfo __attribute__((swift_name("imageInfo")));
@end

__attribute__((swift_name("SkikoRect")))
@interface SharedSkikoRect : SharedBase
- (instancetype)initWithLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom __attribute__((swift_name("init(left:top:right:bottom:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoRectCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoRect *)inflateSpread:(float)spread __attribute__((swift_name("inflate(spread:)")));
- (SharedSkikoRect * _Nullable)intersectOther:(SharedSkikoRect *)other __attribute__((swift_name("intersect(other:)")));
- (SharedSkikoRect *)offsetVec:(SharedSkikoPoint *)vec __attribute__((swift_name("offset(vec:)")));
- (SharedSkikoRect *)offsetDx:(float)dx dy:(float)dy __attribute__((swift_name("offset(dx:dy:)")));
- (SharedSkikoRect *)scaleScale:(float)scale __attribute__((swift_name("scale(scale:)")));
- (SharedSkikoRect *)scaleSx:(float)sx sy:(float)sy __attribute__((swift_name("scale(sx:sy:)")));
- (SharedSkikoIRect *)toIRect __attribute__((swift_name("toIRect()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottom __attribute__((swift_name("bottom")));
@property (readonly) float height_ __attribute__((swift_name("height_")));
@property (readonly, getter=isEmpty_) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) float left __attribute__((swift_name("left")));
@property (readonly) float right __attribute__((swift_name("right")));
@property (readonly) float top __attribute__((swift_name("top")));
@property (readonly) float width_ __attribute__((swift_name("width_")));
@end

__attribute__((swift_name("SkikoSamplingMode")))
@protocol SharedSkikoSamplingMode
@required
- (int64_t)_pack __attribute__((swift_name("_pack()"))) __attribute__((deprecated("Long can't be used because Long is an object in kotlin/js. Consider using _packedInt1 and _packedInt2")));
- (int32_t)_packedInt1 __attribute__((swift_name("_packedInt1()")));
- (int32_t)_packedInt2 __attribute__((swift_name("_packedInt2()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix33")))
@interface SharedSkikoMatrix33 : SharedBase
- (instancetype)initWithMat:(SharedKotlinFloatArray *)mat __attribute__((swift_name("init(mat:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoMatrix33Companion *companion __attribute__((swift_name("companion")));
- (SharedSkikoMatrix44 *)asMatrix44 __attribute__((swift_name("asMatrix44()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoMatrix33 *)makeConcatOther:(SharedSkikoMatrix33 *)other __attribute__((swift_name("makeConcat(other:)")));
- (SharedSkikoMatrix33 *)makePreScaleSx:(float)sx sy:(float)sy __attribute__((swift_name("makePreScale(sx:sy:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinFloatArray *mat __attribute__((swift_name("mat")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRuntimeShaderBuilder")))
@interface SharedSkikoRuntimeShaderBuilder : SharedSkikoManaged
- (instancetype)initWithEffect:(SharedSkikoRuntimeEffect *)effect __attribute__((swift_name("init(effect:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoRuntimeShaderBuilderCompanion *companion __attribute__((swift_name("companion")));
- (void)childName:(NSString *)name colorFilter:(SharedSkikoColorFilter *)colorFilter __attribute__((swift_name("child(name:colorFilter:)")));
- (void)childName:(NSString *)name shader:(SharedSkikoShader *)shader __attribute__((swift_name("child(name:shader:)")));
- (SharedSkikoShader *)makeShaderLocalMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(localMatrix:)")));
- (void)uniformName:(NSString *)name value:(float)value __attribute__((swift_name("uniform(name:value:)")));
- (void)uniformName:(NSString *)name value_:(SharedKotlinFloatArray *)value __attribute__((swift_name("uniform(name:value_:)")));
- (void)uniformName:(NSString *)name value__:(int32_t)value __attribute__((swift_name("uniform(name:value__:)")));
- (void)uniformName:(NSString *)name value___:(SharedSkikoMatrix22 *)value __attribute__((swift_name("uniform(name:value___:)")));
- (void)uniformName:(NSString *)name value____:(SharedSkikoMatrix33 *)value __attribute__((swift_name("uniform(name:value____:)")));
- (void)uniformName:(NSString *)name value_____:(SharedSkikoMatrix44 *)value __attribute__((swift_name("uniform(name:value_____:)")));
- (void)uniformName:(NSString *)name value1:(float)value1 value2:(float)value2 __attribute__((swift_name("uniform(name:value1:value2:)")));
- (void)uniformName:(NSString *)name value1:(int32_t)value1 value2_:(int32_t)value2 __attribute__((swift_name("uniform(name:value1:value2_:)")));
- (void)uniformName:(NSString *)name value1:(float)value1 value2:(float)value2 value3:(float)value3 __attribute__((swift_name("uniform(name:value1:value2:value3:)")));
- (void)uniformName:(NSString *)name value1:(int32_t)value1 value2:(int32_t)value2 value3_:(int32_t)value3 __attribute__((swift_name("uniform(name:value1:value2:value3_:)")));
- (void)uniformName:(NSString *)name value1:(float)value1 value2:(float)value2 value3:(float)value3 value4:(float)value4 __attribute__((swift_name("uniform(name:value1:value2:value3:value4:)")));
- (void)uniformName:(NSString *)name value1:(int32_t)value1 value2:(int32_t)value2 value3:(int32_t)value3 value4_:(int32_t)value4 __attribute__((swift_name("uniform(name:value1:value2:value3:value4_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFilterBlurMode")))
@interface SharedSkikoFilterBlurMode : SharedKotlinEnum<SharedSkikoFilterBlurMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFilterBlurMode *normal __attribute__((swift_name("normal")));
@property (class, readonly) SharedSkikoFilterBlurMode *solid __attribute__((swift_name("solid")));
@property (class, readonly) SharedSkikoFilterBlurMode *outer __attribute__((swift_name("outer")));
@property (class, readonly) SharedSkikoFilterBlurMode *inner __attribute__((swift_name("inner")));
+ (SharedKotlinArray<SharedSkikoFilterBlurMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFilterBlurMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPath")))
@interface SharedSkikoPath : SharedSkikoManaged <SharedKotlinIterable>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPathCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoPath *)addArcOval:(SharedSkikoRect *)oval startAngle:(float)startAngle sweepAngle:(float)sweepAngle __attribute__((swift_name("addArc(oval:startAngle:sweepAngle:)")));
- (SharedSkikoPath *)addCircleX:(float)x y:(float)y radius:(float)radius dir:(SharedSkikoPathDirection *)dir __attribute__((swift_name("addCircle(x:y:radius:dir:)")));
- (SharedSkikoPath *)addOvalOval:(SharedSkikoRect *)oval dir:(SharedSkikoPathDirection *)dir start:(int32_t)start __attribute__((swift_name("addOval(oval:dir:start:)")));
- (SharedSkikoPath *)addPathSrc:(SharedSkikoPath * _Nullable)src extend:(BOOL)extend __attribute__((swift_name("addPath(src:extend:)")));
- (SharedSkikoPath *)addPathSrc:(SharedSkikoPath * _Nullable)src matrix:(SharedSkikoMatrix33 *)matrix extend:(BOOL)extend __attribute__((swift_name("addPath(src:matrix:extend:)")));
- (SharedSkikoPath *)addPathSrc:(SharedSkikoPath * _Nullable)src dx:(float)dx dy:(float)dy extend:(BOOL)extend __attribute__((swift_name("addPath(src:dx:dy:extend:)")));
- (SharedSkikoPath *)addPolyPts:(SharedKotlinArray<SharedSkikoPoint *> *)pts close:(BOOL)close __attribute__((swift_name("addPoly(pts:close:)")));
- (SharedSkikoPath *)addPolyPts:(SharedKotlinFloatArray *)pts close_:(BOOL)close __attribute__((swift_name("addPoly(pts:close_:)")));
- (SharedSkikoPath *)addRRectRrect:(SharedSkikoRRect *)rrect dir:(SharedSkikoPathDirection *)dir start:(int32_t)start __attribute__((swift_name("addRRect(rrect:dir:start:)")));
- (SharedSkikoPath *)addRectRect:(SharedSkikoRect *)rect dir:(SharedSkikoPathDirection *)dir start:(int32_t)start __attribute__((swift_name("addRect(rect:dir:start:)")));
- (SharedSkikoPath *)arcToOval:(SharedSkikoRect *)oval startAngle:(float)startAngle sweepAngle:(float)sweepAngle forceMoveTo:(BOOL)forceMoveTo __attribute__((swift_name("arcTo(oval:startAngle:sweepAngle:forceMoveTo:)")));
- (SharedSkikoPath *)closePath __attribute__((swift_name("closePath()")));
- (SharedSkikoRect *)computeTightBounds __attribute__((swift_name("computeTightBounds()")));
- (SharedSkikoPath *)conicToP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 w:(float)w __attribute__((swift_name("conicTo(p1:p2:w:)")));
- (SharedSkikoPath *)conicToX1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 w:(float)w __attribute__((swift_name("conicTo(x1:y1:x2:y2:w:)")));
- (BOOL)conservativelyContainsRectRect:(SharedSkikoRect *)rect __attribute__((swift_name("conservativelyContainsRect(rect:)")));
- (BOOL)containsP:(SharedSkikoPoint *)p __attribute__((swift_name("contains(p:)")));
- (BOOL)containsX:(float)x y:(float)y __attribute__((swift_name("contains(x:y:)")));
- (SharedSkikoPath *)cubicToP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 p3:(SharedSkikoPoint *)p3 __attribute__((swift_name("cubicTo(p1:p2:p3:)")));
- (SharedSkikoPath *)cubicToX1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 x3:(float)x3 y3:(float)y3 __attribute__((swift_name("cubicTo(x1:y1:x2:y2:x3:y3:)")));
- (SharedSkikoPath *)dump __attribute__((swift_name("dump()")));
- (SharedSkikoPath *)dumpHex __attribute__((swift_name("dumpHex()")));
- (SharedSkikoPath *)ellipticalArcToR:(SharedSkikoPoint *)r xAxisRotate:(float)xAxisRotate arc:(SharedSkikoPathEllipseArc *)arc direction:(SharedSkikoPathDirection *)direction xy:(SharedSkikoPoint *)xy __attribute__((swift_name("ellipticalArcTo(r:xAxisRotate:arc:direction:xy:)")));
- (SharedSkikoPath *)ellipticalArcToRx:(float)rx ry:(float)ry xAxisRotate:(float)xAxisRotate arc:(SharedSkikoPathEllipseArc *)arc direction:(SharedSkikoPathDirection *)direction x:(float)x y:(float)y __attribute__((swift_name("ellipticalArcTo(rx:ry:xAxisRotate:arc:direction:x:y:)")));
- (SharedSkikoPoint *)getPointIndex:(int32_t)index __attribute__((swift_name("getPoint(index:)")));
- (int32_t)getPointsPoints:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)points max:(int32_t)max __attribute__((swift_name("getPoints(points:max:)")));
- (int32_t)getVerbsVerbs:(SharedKotlinArray<SharedSkikoPathVerb *> * _Nullable)verbs max:(int32_t)max __attribute__((swift_name("getVerbs(verbs:max:)")));
- (SharedSkikoPath *)incReserveExtraPtCount:(int32_t)extraPtCount __attribute__((swift_name("incReserve(extraPtCount:)")));
- (BOOL)isInterpolatableCompare:(SharedSkikoPath * _Nullable)compare __attribute__((swift_name("isInterpolatable(compare:)")));
- (SharedSkikoPathSegmentIterator *)iterator __attribute__((swift_name("iterator()")));
- (SharedSkikoPathSegmentIterator *)iteratorForceClose:(BOOL)forceClose __attribute__((swift_name("iterator(forceClose:)")));
- (SharedSkikoPath *)lineToP:(SharedSkikoPoint *)p __attribute__((swift_name("lineTo(p:)")));
- (SharedSkikoPath *)lineToX:(float)x y:(float)y __attribute__((swift_name("lineTo(x:y:)")));
- (SharedSkikoPath *)makeLerpEnding:(SharedSkikoPath * _Nullable)ending weight:(float)weight __attribute__((swift_name("makeLerp(ending:weight:)")));
- (SharedSkikoPath *)moveToP:(SharedSkikoPoint *)p __attribute__((swift_name("moveTo(p:)")));
- (SharedSkikoPath *)moveToX:(float)x y:(float)y __attribute__((swift_name("moveTo(x:y:)")));
- (SharedSkikoPath *)offsetDx:(float)dx dy:(float)dy dst:(SharedSkikoPath * _Nullable)dst __attribute__((swift_name("offset(dx:dy:dst:)")));
- (SharedSkikoPath *)quadToP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 __attribute__((swift_name("quadTo(p1:p2:)")));
- (SharedSkikoPath *)quadToX1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 __attribute__((swift_name("quadTo(x1:y1:x2:y2:)")));
- (SharedSkikoPath *)rConicToDx1:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 w:(float)w __attribute__((swift_name("rConicTo(dx1:dy1:dx2:dy2:w:)")));
- (SharedSkikoPath *)rCubicToDx1:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 dx3:(float)dx3 dy3:(float)dy3 __attribute__((swift_name("rCubicTo(dx1:dy1:dx2:dy2:dx3:dy3:)")));
- (SharedSkikoPath *)rEllipticalArcToRx:(float)rx ry:(float)ry xAxisRotate:(float)xAxisRotate arc:(SharedSkikoPathEllipseArc *)arc direction:(SharedSkikoPathDirection *)direction dx:(float)dx dy:(float)dy __attribute__((swift_name("rEllipticalArcTo(rx:ry:xAxisRotate:arc:direction:dx:dy:)")));
- (SharedSkikoPath *)rLineToDx:(float)dx dy:(float)dy __attribute__((swift_name("rLineTo(dx:dy:)")));
- (SharedSkikoPath *)rMoveToDx:(float)dx dy:(float)dy __attribute__((swift_name("rMoveTo(dx:dy:)")));
- (SharedSkikoPath *)rQuadToDx1:(float)dx1 dy1:(float)dy1 dx2:(float)dx2 dy2:(float)dy2 __attribute__((swift_name("rQuadTo(dx1:dy1:dx2:dy2:)")));
- (SharedSkikoPath *)reset __attribute__((swift_name("reset()")));
- (SharedSkikoPath *)reverseAddPathSrc:(SharedSkikoPath * _Nullable)src __attribute__((swift_name("reverseAddPath(src:)")));
- (SharedSkikoPath *)rewind __attribute__((swift_name("rewind()")));
- (SharedKotlinByteArray *)serializeToBytes __attribute__((swift_name("serializeToBytes()")));
- (SharedSkikoPath *)setLastPtX:(float)x y:(float)y __attribute__((swift_name("setLastPt(x:y:)")));
- (SharedSkikoPath *)setVolatileIsVolatile:(BOOL)isVolatile __attribute__((swift_name("setVolatile(isVolatile:)")));
- (SharedSkikoPath *)swapOther:(SharedSkikoPath * _Nullable)other __attribute__((swift_name("swap(other:)")));
- (SharedSkikoPath *)tangentArcToP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 radius:(float)radius __attribute__((swift_name("tangentArcTo(p1:p2:radius:)")));
- (SharedSkikoPath *)tangentArcToX1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 radius:(float)radius __attribute__((swift_name("tangentArcTo(x1:y1:x2:y2:radius:)")));
- (SharedSkikoPath *)transformMatrix:(SharedSkikoMatrix33 *)matrix applyPerspectiveClip:(BOOL)applyPerspectiveClip __attribute__((swift_name("transform(matrix:applyPerspectiveClip:)")));
- (SharedSkikoPath *)transformMatrix:(SharedSkikoMatrix33 *)matrix dst:(SharedSkikoPath * _Nullable)dst applyPerspectiveClip:(BOOL)applyPerspectiveClip __attribute__((swift_name("transform(matrix:dst:applyPerspectiveClip:)")));
- (SharedSkikoPath *)updateBoundsCache __attribute__((swift_name("updateBoundsCache()")));
@property (readonly) void * _Nullable approximateBytesUsed __attribute__((swift_name("approximateBytesUsed")));
@property (readonly) SharedKotlinArray<SharedSkikoPoint *> * _Nullable asLine __attribute__((swift_name("asLine")));
@property (readonly) SharedSkikoRect *bounds __attribute__((swift_name("bounds")));
@property SharedSkikoPathFillMode *fillMode __attribute__((swift_name("fillMode")));
@property (readonly) int32_t generationId __attribute__((swift_name("generationId")));
@property (readonly) BOOL isConvex __attribute__((swift_name("isConvex")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isFinite __attribute__((swift_name("isFinite")));
@property (readonly) BOOL isLastContourClosed __attribute__((swift_name("isLastContourClosed")));
@property (readonly) SharedSkikoRect * _Nullable isOval __attribute__((swift_name("isOval")));
@property (readonly) SharedSkikoRRect * _Nullable isRRect __attribute__((swift_name("isRRect")));
@property (readonly) SharedSkikoRect * _Nullable isRect __attribute__((swift_name("isRect")));
@property (readonly) BOOL isValid __attribute__((swift_name("isValid")));
@property BOOL isVolatile __attribute__((swift_name("isVolatile")));
@property SharedSkikoPoint *lastPt __attribute__((swift_name("lastPt")));
@property (readonly) SharedKotlinArray<SharedSkikoPoint *> *points __attribute__((swift_name("points")));
@property (readonly) int32_t pointsCount __attribute__((swift_name("pointsCount")));
@property (readonly) int32_t segmentMasks __attribute__((swift_name("segmentMasks")));
@property (readonly) SharedKotlinArray<SharedSkikoPathVerb *> *verbs __attribute__((swift_name("verbs")));
@property (readonly) int32_t verbsCount __attribute__((swift_name("verbsCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathEffect.Style")))
@interface SharedSkikoPathEffectStyle : SharedKotlinEnum<SharedSkikoPathEffectStyle *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathEffectStyle *translate __attribute__((swift_name("translate")));
@property (class, readonly) SharedSkikoPathEffectStyle *rotate __attribute__((swift_name("rotate")));
@property (class, readonly) SharedSkikoPathEffectStyle *morph __attribute__((swift_name("morph")));
+ (SharedKotlinArray<SharedSkikoPathEffectStyle *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathEffectStyle *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoISize.Companion")))
@interface SharedSkikoISizeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoISizeCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoISize *)makeW:(int32_t)w h:(int32_t)h __attribute__((swift_name("make(w:h:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoISize *)makeEmpty __attribute__((swift_name("makeEmpty()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPoint.Companion")))
@interface SharedSkikoPointCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPointCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinFloatArray * _Nullable)flattenArrayPts:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)pts __attribute__((swift_name("flattenArray(pts:)")));
- (SharedKotlinArray<SharedSkikoPoint *> * _Nullable)fromArrayPts:(SharedKotlinFloatArray * _Nullable)pts __attribute__((swift_name("fromArray(pts:)")));
@property (readonly) SharedSkikoPoint *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((swift_name("KotlinIntIterator")))
@interface SharedKotlinIntIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedInt *)next __attribute__((swift_name("next()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoGradientStyle.Companion")))
@interface SharedSkikoGradientStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoGradientStyleCompanion *shared __attribute__((swift_name("shared")));
@property SharedSkikoGradientStyle *DEFAULT __attribute__((swift_name("DEFAULT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoIRect.Companion")))
@interface SharedSkikoIRectCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoIRectCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoIRect *)makeLTRBL:(int32_t)l t:(int32_t)t r:(int32_t)r b:(int32_t)b __attribute__((swift_name("makeLTRB(l:t:r:b:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoIRect *)makeWHW:(int32_t)w h:(int32_t)h __attribute__((swift_name("makeWH(w:h:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoIRect *)makeXYWHL:(int32_t)l t:(int32_t)t w:(int32_t)w h:(int32_t)h __attribute__((swift_name("makeXYWH(l:t:w:h:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoIPoint")))
@interface SharedSkikoIPoint : SharedBase
- (instancetype)initWithX:(int32_t)x y:(int32_t)y __attribute__((swift_name("init(x:y:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoIPointCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoIPoint *)offsetVec:(SharedSkikoIPoint *)vec __attribute__((swift_name("offset(vec:)")));
- (SharedSkikoIPoint *)offsetDx:(int32_t)dx dy:(int32_t)dy __attribute__((swift_name("offset(dx:dy:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) int32_t x __attribute__((swift_name("x")));
@property (readonly) int32_t y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorAlphaType")))
@interface SharedSkikoColorAlphaType : SharedKotlinEnum<SharedSkikoColorAlphaType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoColorAlphaType *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedSkikoColorAlphaType *opaque __attribute__((swift_name("opaque")));
@property (class, readonly) SharedSkikoColorAlphaType *premul __attribute__((swift_name("premul")));
@property (class, readonly) SharedSkikoColorAlphaType *unpremul __attribute__((swift_name("unpremul")));
+ (SharedKotlinArray<SharedSkikoColorAlphaType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoColorAlphaType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorInfo")))
@interface SharedSkikoColorInfo : SharedBase
- (instancetype)initWithColorType:(SharedSkikoColorType *)colorType alphaType:(SharedSkikoColorAlphaType *)alphaType colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("init(colorType:alphaType:colorSpace:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoColorInfoCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoColorInfo *)withAlphaType_alphaType:(SharedSkikoColorAlphaType *)_alphaType __attribute__((swift_name("withAlphaType(_alphaType:)")));
- (SharedSkikoColorInfo *)withColorSpace_colorSpace:(SharedSkikoColorSpace * _Nullable)_colorSpace __attribute__((swift_name("withColorSpace(_colorSpace:)")));
- (SharedSkikoColorInfo *)withColorType_colorType:(SharedSkikoColorType *)_colorType __attribute__((swift_name("withColorType(_colorType:)")));
@property (readonly) SharedSkikoColorAlphaType *alphaType __attribute__((swift_name("alphaType")));
@property (readonly) int32_t bytesPerPixel __attribute__((swift_name("bytesPerPixel")));
@property (readonly) SharedSkikoColorSpace * _Nullable colorSpace __attribute__((swift_name("colorSpace")));
@property (readonly) SharedSkikoColorType *colorType __attribute__((swift_name("colorType")));
@property (readonly) BOOL isGammaCloseToSRGB __attribute__((swift_name("isGammaCloseToSRGB")));
@property (readonly) BOOL isOpaque __attribute__((swift_name("isOpaque")));
@property (readonly) int32_t shiftPerPixel __attribute__((swift_name("shiftPerPixel")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorType")))
@interface SharedSkikoColorType : SharedKotlinEnum<SharedSkikoColorType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoColorTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedSkikoColorType *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedSkikoColorType *alpha8 __attribute__((swift_name("alpha8")));
@property (class, readonly) SharedSkikoColorType *rgb565 __attribute__((swift_name("rgb565")));
@property (class, readonly) SharedSkikoColorType *argb4444 __attribute__((swift_name("argb4444")));
@property (class, readonly) SharedSkikoColorType *rgba8888 __attribute__((swift_name("rgba8888")));
@property (class, readonly) SharedSkikoColorType *rgb888x __attribute__((swift_name("rgb888x")));
@property (class, readonly) SharedSkikoColorType *bgra8888 __attribute__((swift_name("bgra8888")));
@property (class, readonly) SharedSkikoColorType *rgba1010102 __attribute__((swift_name("rgba1010102")));
@property (class, readonly) SharedSkikoColorType *bgra1010102 __attribute__((swift_name("bgra1010102")));
@property (class, readonly) SharedSkikoColorType *rgb101010x __attribute__((swift_name("rgb101010x")));
@property (class, readonly) SharedSkikoColorType *bgr101010x __attribute__((swift_name("bgr101010x")));
@property (class, readonly) SharedSkikoColorType *bgr101010xXr __attribute__((swift_name("bgr101010xXr")));
@property (class, readonly) SharedSkikoColorType *bgra10101010Xr __attribute__((swift_name("bgra10101010Xr")));
@property (class, readonly) SharedSkikoColorType *rgba10x6 __attribute__((swift_name("rgba10x6")));
@property (class, readonly) SharedSkikoColorType *gray8 __attribute__((swift_name("gray8")));
@property (class, readonly) SharedSkikoColorType *rgbaF16norm __attribute__((swift_name("rgbaF16norm")));
@property (class, readonly) SharedSkikoColorType *rgbaF16 __attribute__((swift_name("rgbaF16")));
@property (class, readonly) SharedSkikoColorType *rgbaF32 __attribute__((swift_name("rgbaF32")));
@property (class, readonly) SharedSkikoColorType *r8g8Unorm __attribute__((swift_name("r8g8Unorm")));
@property (class, readonly) SharedSkikoColorType *a16Float __attribute__((swift_name("a16Float")));
@property (class, readonly) SharedSkikoColorType *r16g16Float __attribute__((swift_name("r16g16Float")));
@property (class, readonly) SharedSkikoColorType *a16Unorm __attribute__((swift_name("a16Unorm")));
@property (class, readonly) SharedSkikoColorType *r16g16Unorm __attribute__((swift_name("r16g16Unorm")));
@property (class, readonly) SharedSkikoColorType *r16g16b16a16Unorm __attribute__((swift_name("r16g16b16a16Unorm")));
+ (SharedKotlinArray<SharedSkikoColorType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoColorType *> *entries __attribute__((swift_name("entries")));
- (int64_t)computeOffsetX:(int32_t)x y:(int32_t)y rowBytes:(int64_t)rowBytes __attribute__((swift_name("computeOffset(x:y:rowBytes:)")));
- (float)getAColor:(int8_t)color __attribute__((swift_name("getA(color:)")));
- (float)getAColor_:(int32_t)color __attribute__((swift_name("getA(color_:)")));
- (float)getAColor__:(int16_t)color __attribute__((swift_name("getA(color__:)")));
- (float)getBColor:(int8_t)color __attribute__((swift_name("getB(color:)")));
- (float)getBColor_:(int32_t)color __attribute__((swift_name("getB(color_:)")));
- (float)getBColor__:(int16_t)color __attribute__((swift_name("getB(color__:)")));
- (float)getGColor:(int8_t)color __attribute__((swift_name("getG(color:)")));
- (float)getGColor_:(int32_t)color __attribute__((swift_name("getG(color_:)")));
- (float)getGColor__:(int16_t)color __attribute__((swift_name("getG(color__:)")));
- (float)getRColor:(int8_t)color __attribute__((swift_name("getR(color:)")));
- (float)getRColor_:(int32_t)color __attribute__((swift_name("getR(color_:)")));
- (float)getRColor__:(int16_t)color __attribute__((swift_name("getR(color__:)")));
- (SharedSkikoColorAlphaType * _Nullable)validateAlphaTypeAlphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("validateAlphaType(alphaType:)")));
@property (readonly) int32_t bytesPerPixel __attribute__((swift_name("bytesPerPixel")));
@property (readonly) BOOL isAlwaysOpaque __attribute__((swift_name("isAlwaysOpaque")));
@property (readonly) int32_t shiftPerPixel __attribute__((swift_name("shiftPerPixel")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImageInfo")))
@interface SharedSkikoImageInfo : SharedBase
- (instancetype)initWithColorInfo:(SharedSkikoColorInfo *)colorInfo width:(int32_t)width height:(int32_t)height __attribute__((swift_name("init(colorInfo:width:height:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithWidth:(int32_t)width height:(int32_t)height colorType:(SharedSkikoColorType *)colorType alphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("init(width:height:colorType:alphaType:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithWidth:(int32_t)width height:(int32_t)height colorType:(SharedSkikoColorType *)colorType alphaType:(SharedSkikoColorAlphaType *)alphaType colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("init(width:height:colorType:alphaType:colorSpace:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoImageInfoCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)computeByteSizeRowBytes:(int32_t)rowBytes __attribute__((swift_name("computeByteSize(rowBytes:)")));
- (int32_t)computeMinByteSize __attribute__((swift_name("computeMinByteSize()")));
- (int64_t)computeOffsetX:(int32_t)x y:(int32_t)y rowBytes:(int64_t)rowBytes __attribute__((swift_name("computeOffset(x:y:rowBytes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isRowBytesValidRowBytes:(int64_t)rowBytes __attribute__((swift_name("isRowBytesValid(rowBytes:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoImageInfo *)withColorAlphaTypeAlphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("withColorAlphaType(alphaType:)")));
- (SharedSkikoImageInfo *)withColorInfo_colorInfo:(SharedSkikoColorInfo *)_colorInfo __attribute__((swift_name("withColorInfo(_colorInfo:)")));
- (SharedSkikoImageInfo *)withColorSpaceColorSpace:(SharedSkikoColorSpace *)colorSpace __attribute__((swift_name("withColorSpace(colorSpace:)")));
- (SharedSkikoImageInfo *)withColorTypeColorType:(SharedSkikoColorType *)colorType __attribute__((swift_name("withColorType(colorType:)")));
- (SharedSkikoImageInfo *)withHeight_height:(int32_t)_height __attribute__((swift_name("withHeight(_height:)")));
- (SharedSkikoImageInfo *)withWidth_width:(int32_t)_width __attribute__((swift_name("withWidth(_width:)")));
- (SharedSkikoImageInfo *)withWidthHeightWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("withWidthHeight(width:height:)")));
@property (readonly) SharedSkikoIRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) int32_t bytesPerPixel __attribute__((swift_name("bytesPerPixel")));
@property (readonly) SharedSkikoColorAlphaType *colorAlphaType __attribute__((swift_name("colorAlphaType")));
@property (readonly) SharedSkikoColorInfo *colorInfo __attribute__((swift_name("colorInfo")));
@property (readonly) SharedSkikoColorSpace * _Nullable colorSpace __attribute__((swift_name("colorSpace")));
@property (readonly) SharedSkikoColorType *colorType __attribute__((swift_name("colorType")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isGammaCloseToSRGB __attribute__((swift_name("isGammaCloseToSRGB")));
@property (readonly) BOOL isOpaque __attribute__((swift_name("isOpaque")));
@property (readonly) int32_t minRowBytes __attribute__((swift_name("minRowBytes")));
@property (readonly) int32_t shiftPerPixel __attribute__((swift_name("shiftPerPixel")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImage.Companion")))
@interface SharedSkikoImageCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoImageCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoImage *)makeFromBitmapBitmap:(SharedSkikoBitmap *)bitmap __attribute__((swift_name("makeFromBitmap(bitmap:)")));
- (SharedSkikoImage *)makeFromEncodedBytes:(SharedKotlinByteArray *)bytes __attribute__((swift_name("makeFromEncoded(bytes:)")));
- (SharedSkikoImage *)makeFromPixmapPixmap:(SharedSkikoPixmap *)pixmap __attribute__((swift_name("makeFromPixmap(pixmap:)")));
- (SharedSkikoImage *)makeRasterImageInfo:(SharedSkikoImageInfo *)imageInfo bytes:(SharedKotlinByteArray *)bytes rowBytes:(int32_t)rowBytes __attribute__((swift_name("makeRaster(imageInfo:bytes:rowBytes:)")));
- (SharedSkikoImage *)makeRasterImageInfo:(SharedSkikoImageInfo *)imageInfo data:(SharedSkikoData *)data rowBytes:(int32_t)rowBytes __attribute__((swift_name("makeRaster(imageInfo:data:rowBytes:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoData")))
@interface SharedSkikoData : SharedSkikoManaged
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoDataCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedKotlinByteArray *)getBytesOffset:(int32_t)offset length:(int32_t)length __attribute__((swift_name("getBytes(offset:length:)")));
- (SharedSkikoData *)makeCopy __attribute__((swift_name("makeCopy()")));
- (SharedSkikoData *)makeSubsetOffset:(int32_t)offset length:(int32_t)length __attribute__((swift_name("makeSubset(offset:length:)")));
- (void * _Nullable)writableData __attribute__((swift_name("writableData()")));
@property (readonly) SharedKotlinByteArray *bytes __attribute__((swift_name("bytes")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoEncodedImageFormat")))
@interface SharedSkikoEncodedImageFormat : SharedKotlinEnum<SharedSkikoEncodedImageFormat *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoEncodedImageFormat *bmp __attribute__((swift_name("bmp")));
@property (class, readonly) SharedSkikoEncodedImageFormat *gif __attribute__((swift_name("gif")));
@property (class, readonly) SharedSkikoEncodedImageFormat *ico __attribute__((swift_name("ico")));
@property (class, readonly) SharedSkikoEncodedImageFormat *jpeg __attribute__((swift_name("jpeg")));
@property (class, readonly) SharedSkikoEncodedImageFormat *png __attribute__((swift_name("png")));
@property (class, readonly) SharedSkikoEncodedImageFormat *wbmp __attribute__((swift_name("wbmp")));
@property (class, readonly) SharedSkikoEncodedImageFormat *webp __attribute__((swift_name("webp")));
@property (class, readonly) SharedSkikoEncodedImageFormat *pkm __attribute__((swift_name("pkm")));
@property (class, readonly) SharedSkikoEncodedImageFormat *ktx __attribute__((swift_name("ktx")));
@property (class, readonly) SharedSkikoEncodedImageFormat *astc __attribute__((swift_name("astc")));
@property (class, readonly) SharedSkikoEncodedImageFormat *dng __attribute__((swift_name("dng")));
@property (class, readonly) SharedSkikoEncodedImageFormat *heif __attribute__((swift_name("heif")));
+ (SharedKotlinArray<SharedSkikoEncodedImageFormat *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoEncodedImageFormat *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPixmap")))
@interface SharedSkikoPixmap : SharedSkikoManaged
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPixmapCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)computeByteSize __attribute__((swift_name("computeByteSize()")));
- (BOOL)computeIsOpaque __attribute__((swift_name("computeIsOpaque()")));
- (BOOL)eraseColor:(int32_t)color __attribute__((swift_name("erase(color:)")));
- (BOOL)eraseColor:(int32_t)color subset:(SharedSkikoIRect *)subset __attribute__((swift_name("erase(color:subset:)")));
- (BOOL)extractSubsetSubsetPtr:(void * _Nullable)subsetPtr area:(SharedSkikoIRect *)area __attribute__((swift_name("extractSubset(subsetPtr:area:)")));
- (BOOL)extractSubsetSubset:(SharedSkikoPixmap *)subset area:(SharedSkikoIRect *)area __attribute__((swift_name("extractSubset(subset:area:)")));
- (void * _Nullable)getAddrX:(int32_t)x y:(int32_t)y __attribute__((swift_name("getAddr(x:y:)")));
- (float)getAlphaFX:(int32_t)x y:(int32_t)y __attribute__((swift_name("getAlphaF(x:y:)")));
- (int32_t)getColorX:(int32_t)x y:(int32_t)y __attribute__((swift_name("getColor(x:y:)")));
- (BOOL)readPixelsPixmap:(SharedSkikoPixmap * _Nullable)pixmap __attribute__((swift_name("readPixels(pixmap:)")));
- (BOOL)readPixelsInfo:(SharedSkikoImageInfo *)info addr:(void * _Nullable)addr rowBytes:(int32_t)rowBytes __attribute__((swift_name("readPixels(info:addr:rowBytes:)")));
- (BOOL)readPixelsPixmap:(SharedSkikoPixmap *)pixmap srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(pixmap:srcX:srcY:)")));
- (BOOL)readPixelsInfo:(SharedSkikoImageInfo *)info addr:(void * _Nullable)addr rowBytes:(int32_t)rowBytes srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(info:addr:rowBytes:srcX:srcY:)")));
- (void)reset __attribute__((swift_name("reset()")));
- (void)resetInfo:(SharedSkikoImageInfo *)info buffer:(SharedSkikoData *)buffer rowBytes:(int32_t)rowBytes __attribute__((swift_name("reset(info:buffer:rowBytes:)")));
- (void)resetInfo:(SharedSkikoImageInfo *)info addr:(void * _Nullable)addr rowBytes:(int32_t)rowBytes underlyingMemoryOwner:(SharedSkikoManaged * _Nullable)underlyingMemoryOwner __attribute__((swift_name("reset(info:addr:rowBytes:underlyingMemoryOwner:)")));
- (BOOL)scalePixelsDstPixmap:(SharedSkikoPixmap * _Nullable)dstPixmap samplingMode:(id<SharedSkikoSamplingMode>)samplingMode __attribute__((swift_name("scalePixels(dstPixmap:samplingMode:)")));
- (void)setColorSpaceColorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("setColorSpace(colorSpace:)")));
@property (readonly) void * _Nullable addr __attribute__((swift_name("addr")));
@property (readonly) SharedSkikoData *buffer __attribute__((swift_name("buffer")));
@property (readonly) SharedSkikoImageInfo *info __attribute__((swift_name("info")));
@property (readonly) int32_t rowBytes __attribute__((swift_name("rowBytes")));
@property (readonly) int32_t rowBytesAsPixels __attribute__((swift_name("rowBytesAsPixels")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoBitmap")))
@interface SharedSkikoBitmap : SharedSkikoManaged <SharedSkikoIHasImageInfo>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoBitmapCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)doAllocN32PixelsWidth:(int32_t)width height:(int32_t)height opaque:(BOOL)opaque __attribute__((swift_name("doAllocN32Pixels(width:height:opaque:)")));
- (BOOL)doAllocPixels __attribute__((swift_name("doAllocPixels()")));
- (BOOL)doAllocPixelsImageInfo:(SharedSkikoImageInfo *)imageInfo __attribute__((swift_name("doAllocPixels(imageInfo:)")));
- (BOOL)doAllocPixelsInfo:(SharedSkikoImageInfo *)info rowBytes:(int32_t)rowBytes __attribute__((swift_name("doAllocPixels(info:rowBytes:)")));
- (BOOL)doAllocPixelsFlagsImageInfo:(SharedSkikoImageInfo *)imageInfo zeroPixels:(BOOL)zeroPixels __attribute__((swift_name("doAllocPixelsFlags(imageInfo:zeroPixels:)")));
- (int32_t)computeByteSize __attribute__((swift_name("computeByteSize()")));
- (BOOL)computeIsOpaque __attribute__((swift_name("computeIsOpaque()")));
- (BOOL)drawsNothing __attribute__((swift_name("drawsNothing()")));
- (SharedSkikoBitmap *)eraseColor:(int32_t)color __attribute__((swift_name("erase(color:)")));
- (SharedSkikoBitmap *)eraseColor:(int32_t)color area:(SharedSkikoIRect *)area __attribute__((swift_name("erase(color:area:)")));
- (BOOL)extractAlphaDst:(SharedSkikoBitmap *)dst __attribute__((swift_name("extractAlpha(dst:)")));
- (SharedSkikoIPoint * _Nullable)extractAlphaDst:(SharedSkikoBitmap *)dst paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("extractAlpha(dst:paint:)")));
- (BOOL)extractSubsetDst:(SharedSkikoBitmap *)dst subset:(SharedSkikoIRect *)subset __attribute__((swift_name("extractSubset(dst:subset:)")));
- (float)getAlphafX:(int32_t)x y:(int32_t)y __attribute__((swift_name("getAlphaf(x:y:)")));
- (int32_t)getColorX:(int32_t)x y:(int32_t)y __attribute__((swift_name("getColor(x:y:)")));
- (BOOL)installPixelsPixels:(SharedKotlinByteArray * _Nullable)pixels __attribute__((swift_name("installPixels(pixels:)")));
- (BOOL)installPixelsInfo:(SharedSkikoImageInfo *)info pixels:(SharedKotlinByteArray * _Nullable)pixels rowBytes:(int32_t)rowBytes __attribute__((swift_name("installPixels(info:pixels:rowBytes:)")));
- (SharedSkikoBitmap *)makeClone __attribute__((swift_name("makeClone()")));
- (SharedSkikoShader *)makeShaderLocalMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(localMatrix:)")));
- (SharedSkikoShader *)makeShaderTmx:(SharedSkikoFilterTileMode *)tmx tmy:(SharedSkikoFilterTileMode *)tmy localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(tmx:tmy:localMatrix:)")));
- (SharedSkikoShader *)makeShaderTmx:(SharedSkikoFilterTileMode *)tmx tmy:(SharedSkikoFilterTileMode *)tmy sampling:(id<SharedSkikoSamplingMode>)sampling localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(tmx:tmy:sampling:localMatrix:)")));
- (SharedSkikoBitmap *)notifyPixelsChanged __attribute__((swift_name("notifyPixelsChanged()")));
- (SharedSkikoPixmap * _Nullable)peekPixels __attribute__((swift_name("peekPixels()")));
- (SharedKotlinByteArray * _Nullable)readPixelsDstInfo:(SharedSkikoImageInfo *)dstInfo dstRowBytes:(int32_t)dstRowBytes srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(dstInfo:dstRowBytes:srcX:srcY:)")));
- (SharedSkikoBitmap *)reset __attribute__((swift_name("reset()")));
- (BOOL)setAlphaTypeAlphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("setAlphaType(alphaType:)")));
- (BOOL)setImageInfoImageInfo:(SharedSkikoImageInfo *)imageInfo __attribute__((swift_name("setImageInfo(imageInfo:)")));
- (BOOL)setImageInfoImageInfo:(SharedSkikoImageInfo *)imageInfo rowBytes:(int32_t)rowBytes __attribute__((swift_name("setImageInfo(imageInfo:rowBytes:)")));
- (SharedSkikoBitmap *)setImmutable __attribute__((swift_name("setImmutable()")));
- (SharedSkikoBitmap *)setPixelRefPixelRef:(SharedSkikoPixelRef * _Nullable)pixelRef dx:(int32_t)dx dy:(int32_t)dy __attribute__((swift_name("setPixelRef(pixelRef:dx:dy:)")));
- (void)swapOther:(SharedSkikoBitmap *)other __attribute__((swift_name("swap(other:)")));
@property (readonly) SharedSkikoIRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) int32_t generationId __attribute__((swift_name("generationId")));
@property (readonly) SharedSkikoImageInfo *imageInfo __attribute__((swift_name("imageInfo")));
@property (readonly) BOOL isImmutable __attribute__((swift_name("isImmutable")));
@property (readonly) BOOL isNull __attribute__((swift_name("isNull")));
@property (readonly) BOOL isReadyToDraw __attribute__((swift_name("isReadyToDraw")));
@property (readonly) SharedSkikoPixelRef * _Nullable pixelRef __attribute__((swift_name("pixelRef")));
@property (readonly) SharedSkikoIPoint *pixelRefOrigin __attribute__((swift_name("pixelRefOrigin")));
@property (readonly) int32_t rowBytes __attribute__((swift_name("rowBytes")));
@property (readonly) int32_t rowBytesAsPixels __attribute__((swift_name("rowBytesAsPixels")));
@property (readonly) SharedSkikoIRect *subset __attribute__((swift_name("subset")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoDirectContext")))
@interface SharedSkikoDirectContext : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoDirectContextCompanion *companion __attribute__((swift_name("companion")));
- (void)abandon __attribute__((swift_name("abandon()")));
- (SharedSkikoDirectContext *)flush __attribute__((swift_name("flush()")));
- (SharedSkikoDirectContext *)flushSurface:(SharedSkikoSurface *)surface __attribute__((swift_name("flush(surface:)")));
- (void)flushAndSubmitSurface:(SharedSkikoSurface *)surface syncCpu:(BOOL)syncCpu __attribute__((swift_name("flushAndSubmit(surface:syncCpu:)")));
- (SharedSkikoDirectContext *)resetAll __attribute__((swift_name("resetAll()")));
- (SharedSkikoDirectContext *)resetGLStates:(SharedKotlinArray<SharedSkikoGLBackendState *> *)states __attribute__((swift_name("resetGL(states:)")));
- (SharedSkikoDirectContext *)resetGLAll __attribute__((swift_name("resetGLAll()")));
- (void)submitSyncCpu:(BOOL)syncCpu __attribute__((swift_name("submit(syncCpu:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRect.Companion")))
@interface SharedSkikoRectCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRectCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRect *)makeLTRBL:(float)l t:(float)t r:(float)r b:(float)b __attribute__((swift_name("makeLTRB(l:t:r:b:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRect *)makeWHSize:(SharedSkikoPoint *)size __attribute__((swift_name("makeWH(size:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRect *)makeWHW:(float)w h:(float)h __attribute__((swift_name("makeWH(w:h:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRect *)makeXYWHL:(float)l t:(float)t w:(float)w h:(float)h __attribute__((swift_name("makeXYWH(l:t:w:h:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix33.Companion")))
@interface SharedSkikoMatrix33Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoMatrix33Companion *shared __attribute__((swift_name("shared")));
- (SharedSkikoMatrix33 *)makeRotateDeg:(float)deg __attribute__((swift_name("makeRotate(deg:)")));
- (SharedSkikoMatrix33 *)makeRotateDeg:(float)deg pivot:(SharedSkikoPoint *)pivot __attribute__((swift_name("makeRotate(deg:pivot:)")));
- (SharedSkikoMatrix33 *)makeRotateDeg:(float)deg pivotx:(float)pivotx pivoty:(float)pivoty __attribute__((swift_name("makeRotate(deg:pivotx:pivoty:)")));
- (SharedSkikoMatrix33 *)makeScaleS:(float)s __attribute__((swift_name("makeScale(s:)")));
- (SharedSkikoMatrix33 *)makeScaleSx:(float)sx sy:(float)sy __attribute__((swift_name("makeScale(sx:sy:)")));
- (SharedSkikoMatrix33 *)makeSkewSx:(float)sx sy:(float)sy __attribute__((swift_name("makeSkew(sx:sy:)")));
- (SharedSkikoMatrix33 *)makeTranslateDx:(float)dx dy:(float)dy __attribute__((swift_name("makeTranslate(dx:dy:)")));
@property (readonly) SharedSkikoMatrix33 *IDENTITY __attribute__((swift_name("IDENTITY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix44")))
@interface SharedSkikoMatrix44 : SharedBase
- (instancetype)initWithMat:(SharedKotlinFloatArray *)mat __attribute__((swift_name("init(mat:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoMatrix44Companion *companion __attribute__((swift_name("companion")));
- (SharedSkikoMatrix33 *)asMatrix33 __attribute__((swift_name("asMatrix33()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinFloatArray *mat __attribute__((swift_name("mat")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRuntimeEffect")))
@interface SharedSkikoRuntimeEffect : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoRuntimeEffectCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoShader *)makeShaderUniforms:(SharedSkikoData * _Nullable)uniforms children:(SharedKotlinArray<SharedSkikoShader *> * _Nullable)children localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix __attribute__((swift_name("makeShader(uniforms:children:localMatrix:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRuntimeShaderBuilder.Companion")))
@interface SharedSkikoRuntimeShaderBuilderCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRuntimeShaderBuilderCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix22")))
@interface SharedSkikoMatrix22 : SharedBase
- (instancetype)initWithMat:(SharedKotlinFloatArray *)mat __attribute__((swift_name("init(mat:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoMatrix22Companion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinFloatArray *mat __attribute__((swift_name("mat")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPath.Companion")))
@interface SharedSkikoPathCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPathCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinArray<SharedSkikoPoint *> *)convertConicToQuadsP0:(SharedSkikoPoint *)p0 p1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 w:(float)w pow2:(int32_t)pow2 __attribute__((swift_name("convertConicToQuads(p0:p1:p2:w:pow2:)")));
- (BOOL)isCubicDegenerateP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 p3:(SharedSkikoPoint *)p3 p4:(SharedSkikoPoint *)p4 exact:(BOOL)exact __attribute__((swift_name("isCubicDegenerate(p1:p2:p3:p4:exact:)")));
- (BOOL)isLineDegenerateP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 exact:(BOOL)exact __attribute__((swift_name("isLineDegenerate(p1:p2:exact:)")));
- (BOOL)isQuadDegenerateP1:(SharedSkikoPoint *)p1 p2:(SharedSkikoPoint *)p2 p3:(SharedSkikoPoint *)p3 exact:(BOOL)exact __attribute__((swift_name("isQuadDegenerate(p1:p2:p3:exact:)")));
- (SharedSkikoPath * _Nullable)makeCombiningOne:(SharedSkikoPath *)one two:(SharedSkikoPath *)two op:(SharedSkikoPathOp *)op __attribute__((swift_name("makeCombining(one:two:op:)")));
- (SharedSkikoPath *)makeFromBytesData:(SharedKotlinByteArray *)data __attribute__((swift_name("makeFromBytes(data:)")));
- (SharedSkikoPath *)makeFromSVGStringSvg:(NSString *)svg __attribute__((swift_name("makeFromSVGString(svg:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathDirection")))
@interface SharedSkikoPathDirection : SharedKotlinEnum<SharedSkikoPathDirection *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathDirection *clockwise __attribute__((swift_name("clockwise")));
@property (class, readonly) SharedSkikoPathDirection *counterClockwise __attribute__((swift_name("counterClockwise")));
+ (SharedKotlinArray<SharedSkikoPathDirection *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathDirection *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRRect")))
@interface SharedSkikoRRect : SharedSkikoRect
- (instancetype)initWithLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom __attribute__((swift_name("init(left:top:right:bottom:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoRRectCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedSkikoRect *)inflateSpread:(float)spread __attribute__((swift_name("inflate(spread:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinFloatArray *radii __attribute__((swift_name("radii")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathEllipseArc")))
@interface SharedSkikoPathEllipseArc : SharedKotlinEnum<SharedSkikoPathEllipseArc *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathEllipseArc *smaller __attribute__((swift_name("smaller")));
@property (class, readonly) SharedSkikoPathEllipseArc *larger __attribute__((swift_name("larger")));
+ (SharedKotlinArray<SharedSkikoPathEllipseArc *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathEllipseArc *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathVerb")))
@interface SharedSkikoPathVerb : SharedKotlinEnum<SharedSkikoPathVerb *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathVerb *move __attribute__((swift_name("move")));
@property (class, readonly) SharedSkikoPathVerb *line __attribute__((swift_name("line")));
@property (class, readonly) SharedSkikoPathVerb *quad __attribute__((swift_name("quad")));
@property (class, readonly) SharedSkikoPathVerb *conic __attribute__((swift_name("conic")));
@property (class, readonly) SharedSkikoPathVerb *cubic __attribute__((swift_name("cubic")));
@property (class, readonly) SharedSkikoPathVerb *close __attribute__((swift_name("close")));
@property (class, readonly) SharedSkikoPathVerb *done __attribute__((swift_name("done")));
+ (SharedKotlinArray<SharedSkikoPathVerb *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathVerb *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathSegmentIterator")))
@interface SharedSkikoPathSegmentIterator : SharedSkikoManaged <SharedKotlinMutableIterator>
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPathSegmentIteratorCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (SharedSkikoPathSegment * _Nullable)next __attribute__((swift_name("next()")));
- (void)remove __attribute__((swift_name("remove()")));
@property SharedSkikoPathSegment * _Nullable _nextSegment __attribute__((swift_name("_nextSegment")));
@property (readonly) SharedSkikoPath * _Nullable _path __attribute__((swift_name("_path")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathFillMode")))
@interface SharedSkikoPathFillMode : SharedKotlinEnum<SharedSkikoPathFillMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathFillMode *winding __attribute__((swift_name("winding")));
@property (class, readonly) SharedSkikoPathFillMode *evenOdd __attribute__((swift_name("evenOdd")));
@property (class, readonly) SharedSkikoPathFillMode *inverseWinding __attribute__((swift_name("inverseWinding")));
@property (class, readonly) SharedSkikoPathFillMode *inverseEvenOdd __attribute__((swift_name("inverseEvenOdd")));
+ (SharedKotlinArray<SharedSkikoPathFillMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathFillMode *> *entries __attribute__((swift_name("entries")));
- (SharedSkikoPathFillMode *)inverse __attribute__((swift_name("inverse()")));
@property (readonly) BOOL isInverse __attribute__((swift_name("isInverse")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoIPoint.Companion")))
@interface SharedSkikoIPointCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoIPointCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoIPoint *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorInfo.Companion")))
@interface SharedSkikoColorInfoCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoColorInfoCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoColorInfo *DEFAULT __attribute__((swift_name("DEFAULT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoColorType.Companion")))
@interface SharedSkikoColorTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoColorTypeCompanion *shared __attribute__((swift_name("shared")));
@property SharedSkikoColorType *N32 __attribute__((swift_name("N32")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoImageInfo.Companion")))
@interface SharedSkikoImageInfoCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoImageInfoCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoImageInfo *)createUsing_ptr:(void * _Nullable)_ptr _nGetImageInfo:(void (^)(id _Nullable, id _Nullable, id _Nullable))_nGetImageInfo __attribute__((swift_name("createUsing(_ptr:_nGetImageInfo:)")));
- (SharedSkikoImageInfo *)makeA8Width:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeA8(width:height:)")));
- (SharedSkikoImageInfo *)makeN32Width:(int32_t)width height:(int32_t)height alphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("makeN32(width:height:alphaType:)")));
- (SharedSkikoImageInfo *)makeN32Width:(int32_t)width height:(int32_t)height alphaType:(SharedSkikoColorAlphaType *)alphaType colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("makeN32(width:height:alphaType:colorSpace:)")));
- (SharedSkikoImageInfo *)makeN32PremulWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeN32Premul(width:height:)")));
- (SharedSkikoImageInfo *)makeN32PremulWidth:(int32_t)width height:(int32_t)height colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace __attribute__((swift_name("makeN32Premul(width:height:colorSpace:)")));
- (SharedSkikoImageInfo *)makeS32Width:(int32_t)width height:(int32_t)height alphaType:(SharedSkikoColorAlphaType *)alphaType __attribute__((swift_name("makeS32(width:height:alphaType:)")));
- (SharedSkikoImageInfo *)makeUnknownWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeUnknown(width:height:)")));
@property (readonly) SharedSkikoImageInfo *DEFAULT __attribute__((swift_name("DEFAULT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoData.Companion")))
@interface SharedSkikoDataCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoDataCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoData *)makeEmpty __attribute__((swift_name("makeEmpty()")));
- (SharedSkikoData *)makeFromBytesBytes:(SharedKotlinByteArray *)bytes offset:(int32_t)offset length:(int32_t)length __attribute__((swift_name("makeFromBytes(bytes:offset:length:)")));
- (SharedSkikoData *)makeUninitializedLength:(int32_t)length __attribute__((swift_name("makeUninitialized(length:)")));
- (SharedSkikoData *)makeWithoutCopyMemoryAddr:(void * _Nullable)memoryAddr length:(int32_t)length underlyingMemoryOwner:(SharedSkikoManaged *)underlyingMemoryOwner __attribute__((swift_name("makeWithoutCopy(memoryAddr:length:underlyingMemoryOwner:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPixmap.Companion")))
@interface SharedSkikoPixmapCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPixmapCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoPixmap *)makeInfo:(SharedSkikoImageInfo *)info buffer:(SharedSkikoData *)buffer rowBytes:(int32_t)rowBytes __attribute__((swift_name("make(info:buffer:rowBytes:)")));
- (SharedSkikoPixmap *)makeInfo:(SharedSkikoImageInfo *)info addr:(void * _Nullable)addr rowBytes:(int32_t)rowBytes underlyingMemoryOwner:(SharedSkikoManaged * _Nullable)underlyingMemoryOwner __attribute__((swift_name("make(info:addr:rowBytes:underlyingMemoryOwner:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoBitmap.Companion")))
@interface SharedSkikoBitmapCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoBitmapCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoBitmap *)makeFromImageImage:(SharedSkikoImage *)image __attribute__((swift_name("makeFromImage(image:)")));
- (SharedSkikoBitmap *)makeFromImageImage:(SharedSkikoImage *)image context:(SharedSkikoDirectContext *)context __attribute__((swift_name("makeFromImage(image:context:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPixelRef")))
@interface SharedSkikoPixelRef : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPixelRefCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoPixelRef *)notifyPixelsChanged __attribute__((swift_name("notifyPixelsChanged()")));
- (SharedSkikoPixelRef *)setImmutable __attribute__((swift_name("setImmutable()")));
@property (readonly) int32_t generationId __attribute__((swift_name("generationId")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) BOOL isImmutable __attribute__((swift_name("isImmutable")));
@property (readonly) void * _Nullable rowBytes __attribute__((swift_name("rowBytes")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoDirectContext.Companion")))
@interface SharedSkikoDirectContextCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoDirectContextCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoDirectContext *)makeDirect3DAdapterPtr:(void * _Nullable)adapterPtr devicePtr:(void * _Nullable)devicePtr queuePtr:(void * _Nullable)queuePtr __attribute__((swift_name("makeDirect3D(adapterPtr:devicePtr:queuePtr:)")));
- (SharedSkikoDirectContext *)makeGL __attribute__((swift_name("makeGL()")));
- (SharedSkikoDirectContext *)makeMetalDevicePtr:(void * _Nullable)devicePtr queuePtr:(void * _Nullable)queuePtr __attribute__((swift_name("makeMetal(devicePtr:queuePtr:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoSurface")))
@interface SharedSkikoSurface : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoSurfaceCompanion *companion __attribute__((swift_name("companion")));
- (void)drawCanvas:(SharedSkikoCanvas * _Nullable)canvas x:(int32_t)x y:(int32_t)y paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("draw(canvas:x:y:paint:)")));
- (void)drawCanvas:(SharedSkikoCanvas * _Nullable)canvas x:(int32_t)x y:(int32_t)y samplingMode:(id<SharedSkikoSamplingMode>)samplingMode paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("draw(canvas:x:y:samplingMode:paint:)")));
- (void)flush __attribute__((swift_name("flush()")));
- (void)flushAndSubmit __attribute__((swift_name("flushAndSubmit()")));
- (void)flushAndSubmitSyncCpu:(BOOL)syncCpu __attribute__((swift_name("flushAndSubmit(syncCpu:)")));
- (SharedSkikoImage *)makeImageSnapshot __attribute__((swift_name("makeImageSnapshot()")));
- (SharedSkikoImage * _Nullable)makeImageSnapshotArea:(SharedSkikoIRect *)area __attribute__((swift_name("makeImageSnapshot(area:)")));
- (SharedSkikoSurface * _Nullable)makeSurfaceImageInfo:(SharedSkikoImageInfo *)imageInfo __attribute__((swift_name("makeSurface(imageInfo:)")));
- (SharedSkikoSurface * _Nullable)makeSurfaceWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeSurface(width:height:)")));
- (void)notifyContentWillChangeMode:(SharedSkikoContentChangeMode *)mode __attribute__((swift_name("notifyContentWillChange(mode:)")));
- (BOOL)peekPixelsPixmap:(SharedSkikoPixmap *)pixmap __attribute__((swift_name("peekPixels(pixmap:)")));
- (BOOL)readPixelsBitmap:(SharedSkikoBitmap * _Nullable)bitmap srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(bitmap:srcX:srcY:)")));
- (BOOL)readPixelsPixmap:(SharedSkikoPixmap * _Nullable)pixmap srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(pixmap:srcX:srcY:)")));
- (void)writePixelsBitmap:(SharedSkikoBitmap * _Nullable)bitmap x:(int32_t)x y:(int32_t)y __attribute__((swift_name("writePixels(bitmap:x:y:)")));
- (void)writePixelsPixmap:(SharedSkikoPixmap * _Nullable)pixmap x:(int32_t)x y:(int32_t)y __attribute__((swift_name("writePixels(pixmap:x:y:)")));
@property (readonly) SharedSkikoCanvas *canvas __attribute__((swift_name("canvas")));
@property (readonly) int32_t generationId __attribute__((swift_name("generationId")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) SharedSkikoImageInfo *imageInfo __attribute__((swift_name("imageInfo")));
@property (readonly) BOOL isUnique __attribute__((swift_name("isUnique")));
@property (readonly) SharedSkikoDirectContext * _Nullable recordingContext __attribute__((swift_name("recordingContext")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoGLBackendState")))
@interface SharedSkikoGLBackendState : SharedKotlinEnum<SharedSkikoGLBackendState *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoGLBackendState *renderTarget __attribute__((swift_name("renderTarget")));
@property (class, readonly) SharedSkikoGLBackendState *textureBinding __attribute__((swift_name("textureBinding")));
@property (class, readonly) SharedSkikoGLBackendState *view __attribute__((swift_name("view")));
@property (class, readonly) SharedSkikoGLBackendState *blend __attribute__((swift_name("blend")));
@property (class, readonly) SharedSkikoGLBackendState *msaaEnable __attribute__((swift_name("msaaEnable")));
@property (class, readonly) SharedSkikoGLBackendState *vertex __attribute__((swift_name("vertex")));
@property (class, readonly) SharedSkikoGLBackendState *stencil __attribute__((swift_name("stencil")));
@property (class, readonly) SharedSkikoGLBackendState *pixelStore __attribute__((swift_name("pixelStore")));
@property (class, readonly) SharedSkikoGLBackendState *program __attribute__((swift_name("program")));
@property (class, readonly) SharedSkikoGLBackendState *fixedFunction __attribute__((swift_name("fixedFunction")));
@property (class, readonly) SharedSkikoGLBackendState *misc __attribute__((swift_name("misc")));
@property (class, readonly) SharedSkikoGLBackendState *pathRendering __attribute__((swift_name("pathRendering")));
+ (SharedKotlinArray<SharedSkikoGLBackendState *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoGLBackendState *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix44.Companion")))
@interface SharedSkikoMatrix44Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoMatrix44Companion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoMatrix44 *IDENTITY __attribute__((swift_name("IDENTITY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRuntimeEffect.Companion")))
@interface SharedSkikoRuntimeEffectCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRuntimeEffectCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoRuntimeEffect *)makeForColorFilterSksl:(NSString *)sksl __attribute__((swift_name("makeForColorFilter(sksl:)")));
- (SharedSkikoRuntimeEffect *)makeForShaderSksl:(NSString *)sksl __attribute__((swift_name("makeForShader(sksl:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatrix22.Companion")))
@interface SharedSkikoMatrix22Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoMatrix22Companion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoMatrix22 *IDENTITY __attribute__((swift_name("IDENTITY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathOp")))
@interface SharedSkikoPathOp : SharedKotlinEnum<SharedSkikoPathOp *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPathOp *difference __attribute__((swift_name("difference")));
@property (class, readonly) SharedSkikoPathOp *intersect __attribute__((swift_name("intersect")));
@property (class, readonly) SharedSkikoPathOp *union_ __attribute__((swift_name("union_")));
@property (class, readonly) SharedSkikoPathOp *xor_ __attribute__((swift_name("xor_")));
@property (class, readonly) SharedSkikoPathOp *reverseDifference __attribute__((swift_name("reverseDifference")));
+ (SharedKotlinArray<SharedSkikoPathOp *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPathOp *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRRect.Companion")))
@interface SharedSkikoRRectCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRRectCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRRect *)makeComplexLTRBL:(float)l t:(float)t r:(float)r b:(float)b radii:(SharedKotlinFloatArray *)radii __attribute__((swift_name("makeComplexLTRB(l:t:r:b:radii:)")));
- (SharedSkikoRRect *)makeComplexXYWHL:(float)l t:(float)t w:(float)w h:(float)h radii:(SharedKotlinFloatArray *)radii __attribute__((swift_name("makeComplexXYWH(l:t:w:h:radii:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRRect *)makeLTRBL:(float)l t:(float)t r:(float)r b:(float)b radius:(float)radius __attribute__((swift_name("makeLTRB(l:t:r:b:radius:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRRect *)makeLTRBL:(float)l t:(float)t r:(float)r b:(float)b xRad:(float)xRad yRad:(float)yRad __attribute__((swift_name("makeLTRB(l:t:r:b:xRad:yRad:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRRect *)makeLTRBL:(float)l t:(float)t r:(float)r b:(float)b tlRad:(float)tlRad trRad:(float)trRad brRad:(float)brRad blRad:(float)blRad __attribute__((swift_name("makeLTRB(l:t:r:b:tlRad:trRad:brRad:blRad:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedSkikoRRect *)makeNinePatchLTRBL:(float)l t:(float)t r:(float)r b:(float)b lRad:(float)lRad tRad:(float)tRad rRad:(float)rRad bRad:(float)bRad __attribute__((swift_name("makeNinePatchLTRB(l:t:r:b:lRad:tRad:rRad:bRad:)")));
- (SharedSkikoRRect *)makeNinePatchXYWHL:(float)l t:(float)t w:(float)w h:(float)h lRad:(float)lRad tRad:(float)tRad rRad:(float)rRad bRad:(float)bRad __attribute__((swift_name("makeNinePatchXYWH(l:t:w:h:lRad:tRad:rRad:bRad:)")));
- (SharedSkikoRRect *)makeOvalLTRBL:(float)l t:(float)t r:(float)r b:(float)b __attribute__((swift_name("makeOvalLTRB(l:t:r:b:)")));
- (SharedSkikoRRect *)makeOvalXYWHL:(float)l t:(float)t w:(float)w h:(float)h __attribute__((swift_name("makeOvalXYWH(l:t:w:h:)")));
- (SharedSkikoRRect *)makePillLTRBL:(float)l t:(float)t r:(float)r b:(float)b __attribute__((swift_name("makePillLTRB(l:t:r:b:)")));
- (SharedSkikoRRect *)makePillXYWHL:(float)l t:(float)t w:(float)w h:(float)h __attribute__((swift_name("makePillXYWH(l:t:w:h:)")));
- (SharedSkikoRRect *)makeXYWHL:(float)l t:(float)t w:(float)w h:(float)h radius:(float)radius __attribute__((swift_name("makeXYWH(l:t:w:h:radius:)")));
- (SharedSkikoRRect *)makeXYWHL:(float)l t:(float)t w:(float)w h:(float)h xRad:(float)xRad yRad:(float)yRad __attribute__((swift_name("makeXYWH(l:t:w:h:xRad:yRad:)")));
- (SharedSkikoRRect *)makeXYWHL:(float)l t:(float)t w:(float)w h:(float)h tlRad:(float)tlRad trRad:(float)trRad brRad:(float)brRad blRad:(float)blRad __attribute__((swift_name("makeXYWH(l:t:w:h:tlRad:trRad:brRad:blRad:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathSegmentIterator.Companion")))
@interface SharedSkikoPathSegmentIteratorCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPathSegmentIteratorCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoPathSegmentIterator *)makePath:(SharedSkikoPath * _Nullable)path forceClose:(BOOL)forceClose __attribute__((swift_name("make(path:forceClose:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPathSegment")))
@interface SharedSkikoPathSegment : SharedBase
- (instancetype)initWithVerbOrdinal:(int32_t)verbOrdinal x0:(float)x0 y0:(float)y0 isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(verbOrdinal:x0:y0:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 isCloseLine:(BOOL)isCloseLine isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(x0:y0:x1:y1:isCloseLine:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(x0:y0:x1:y1:x2:y2:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 conicWeight:(float)conicWeight isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(x0:y0:x1:y1:x2:y2:conicWeight:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithVerb:(SharedSkikoPathVerb *)verb p0:(SharedSkikoPoint * _Nullable)p0 p1:(SharedSkikoPoint * _Nullable)p1 p2:(SharedSkikoPoint * _Nullable)p2 p3:(SharedSkikoPoint * _Nullable)p3 conicWeight:(float)conicWeight isCloseLine:(BOOL)isCloseLine isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(verb:p0:p1:p2:p3:conicWeight:isCloseLine:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 x2:(float)x2 y2:(float)y2 x3:(float)x3 y3:(float)y3 isClosedContour:(BOOL)isClosedContour __attribute__((swift_name("init(x0:y0:x1:y1:x2:y2:x3:y3:isClosedContour:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float conicWeight __attribute__((swift_name("conicWeight")));
@property (readonly) BOOL isCloseLine __attribute__((swift_name("isCloseLine")));
@property (readonly) BOOL isClosedContour __attribute__((swift_name("isClosedContour")));
@property (readonly) SharedSkikoPoint * _Nullable p0 __attribute__((swift_name("p0")));
@property (readonly) SharedSkikoPoint * _Nullable p1 __attribute__((swift_name("p1")));
@property (readonly) SharedSkikoPoint * _Nullable p2 __attribute__((swift_name("p2")));
@property (readonly) SharedSkikoPoint * _Nullable p3 __attribute__((swift_name("p3")));
@property (readonly) SharedSkikoPathVerb *verb __attribute__((swift_name("verb")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPixelRef.Companion")))
@interface SharedSkikoPixelRefCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPixelRefCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoSurface.Companion")))
@interface SharedSkikoSurfaceCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoSurfaceCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoSurface * _Nullable)makeFromBackendRenderTargetContext:(SharedSkikoDirectContext *)context rt:(SharedSkikoBackendRenderTarget *)rt origin:(SharedSkikoSurfaceOrigin *)origin colorFormat:(SharedSkikoSurfaceColorFormat *)colorFormat colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeFromBackendRenderTarget(context:rt:origin:colorFormat:colorSpace:surfaceProps:)")));
- (SharedSkikoSurface *)makeFromMTKViewContext:(SharedSkikoDirectContext *)context mtkViewPtr:(void * _Nullable)mtkViewPtr origin:(SharedSkikoSurfaceOrigin *)origin sampleCount:(int32_t)sampleCount colorFormat:(SharedSkikoSurfaceColorFormat *)colorFormat colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeFromMTKView(context:mtkViewPtr:origin:sampleCount:colorFormat:colorSpace:surfaceProps:)")));
- (SharedSkikoSurface *)makeNullWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeNull(width:height:)")));
- (SharedSkikoSurface *)makeRasterImageInfo:(SharedSkikoImageInfo *)imageInfo __attribute__((swift_name("makeRaster(imageInfo:)")));
- (SharedSkikoSurface *)makeRasterImageInfo:(SharedSkikoImageInfo *)imageInfo rowBytes:(int32_t)rowBytes __attribute__((swift_name("makeRaster(imageInfo:rowBytes:)")));
- (SharedSkikoSurface *)makeRasterImageInfo:(SharedSkikoImageInfo *)imageInfo rowBytes:(int32_t)rowBytes surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeRaster(imageInfo:rowBytes:surfaceProps:)")));
- (SharedSkikoSurface *)makeRasterDirectPixmap:(SharedSkikoPixmap *)pixmap __attribute__((swift_name("makeRasterDirect(pixmap:)")));
- (SharedSkikoSurface *)makeRasterDirectPixmap:(SharedSkikoPixmap *)pixmap surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeRasterDirect(pixmap:surfaceProps:)")));
- (SharedSkikoSurface *)makeRasterDirectImageInfo:(SharedSkikoImageInfo *)imageInfo pixelsPtr:(void * _Nullable)pixelsPtr rowBytes:(int32_t)rowBytes __attribute__((swift_name("makeRasterDirect(imageInfo:pixelsPtr:rowBytes:)")));
- (SharedSkikoSurface *)makeRasterDirectImageInfo:(SharedSkikoImageInfo *)imageInfo pixelsPtr:(void * _Nullable)pixelsPtr rowBytes:(int32_t)rowBytes surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeRasterDirect(imageInfo:pixelsPtr:rowBytes:surfaceProps:)")));
- (SharedSkikoSurface *)makeRasterN32PremulWidth:(int32_t)width height:(int32_t)height __attribute__((swift_name("makeRasterN32Premul(width:height:)")));
- (SharedSkikoSurface *)makeRenderTargetContext:(SharedSkikoDirectContext *)context budgeted:(BOOL)budgeted imageInfo:(SharedSkikoImageInfo *)imageInfo __attribute__((swift_name("makeRenderTarget(context:budgeted:imageInfo:)")));
- (SharedSkikoSurface *)makeRenderTargetContext:(SharedSkikoDirectContext *)context budgeted:(BOOL)budgeted imageInfo:(SharedSkikoImageInfo *)imageInfo sampleCount:(int32_t)sampleCount surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeRenderTarget(context:budgeted:imageInfo:sampleCount:surfaceProps:)")));
- (SharedSkikoSurface *)makeRenderTargetContext:(SharedSkikoDirectContext *)context budgeted:(BOOL)budgeted imageInfo:(SharedSkikoImageInfo *)imageInfo sampleCount:(int32_t)sampleCount origin:(SharedSkikoSurfaceOrigin *)origin surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps __attribute__((swift_name("makeRenderTarget(context:budgeted:imageInfo:sampleCount:origin:surfaceProps:)")));
- (SharedSkikoSurface *)makeRenderTargetContext:(SharedSkikoDirectContext *)context budgeted:(BOOL)budgeted imageInfo:(SharedSkikoImageInfo *)imageInfo sampleCount:(int32_t)sampleCount origin:(SharedSkikoSurfaceOrigin *)origin surfaceProps:(SharedSkikoSurfaceProps * _Nullable)surfaceProps shouldCreateWithMips:(BOOL)shouldCreateWithMips __attribute__((swift_name("makeRenderTarget(context:budgeted:imageInfo:sampleCount:origin:surfaceProps:shouldCreateWithMips:)")));
@end

__attribute__((swift_name("SkikoCanvas")))
@interface SharedSkikoCanvas : SharedSkikoManaged
- (instancetype)initWithBitmap:(SharedSkikoBitmap *)bitmap surfaceProps:(SharedSkikoSurfaceProps *)surfaceProps __attribute__((swift_name("init(bitmap:surfaceProps:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoCanvasCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoCanvas *)clearColor:(int32_t)color __attribute__((swift_name("clear(color:)")));
- (SharedSkikoCanvas *)clipPathP:(SharedSkikoPath *)p __attribute__((swift_name("clipPath(p:)")));
- (SharedSkikoCanvas *)clipPathP:(SharedSkikoPath *)p antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipPath(p:antiAlias:)")));
- (SharedSkikoCanvas *)clipPathP:(SharedSkikoPath *)p mode:(SharedSkikoClipMode *)mode __attribute__((swift_name("clipPath(p:mode:)")));
- (SharedSkikoCanvas *)clipPathP:(SharedSkikoPath *)p mode:(SharedSkikoClipMode *)mode antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipPath(p:mode:antiAlias:)")));
- (SharedSkikoCanvas *)clipRRectR:(SharedSkikoRRect *)r __attribute__((swift_name("clipRRect(r:)")));
- (SharedSkikoCanvas *)clipRRectR:(SharedSkikoRRect *)r antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipRRect(r:antiAlias:)")));
- (SharedSkikoCanvas *)clipRRectR:(SharedSkikoRRect *)r mode:(SharedSkikoClipMode *)mode __attribute__((swift_name("clipRRect(r:mode:)")));
- (SharedSkikoCanvas *)clipRRectR:(SharedSkikoRRect *)r mode:(SharedSkikoClipMode *)mode antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipRRect(r:mode:antiAlias:)")));
- (SharedSkikoCanvas *)clipRectR:(SharedSkikoRect *)r __attribute__((swift_name("clipRect(r:)")));
- (SharedSkikoCanvas *)clipRectR:(SharedSkikoRect *)r antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipRect(r:antiAlias:)")));
- (SharedSkikoCanvas *)clipRectR:(SharedSkikoRect *)r mode:(SharedSkikoClipMode *)mode __attribute__((swift_name("clipRect(r:mode:)")));
- (SharedSkikoCanvas *)clipRectR:(SharedSkikoRect *)r mode:(SharedSkikoClipMode *)mode antiAlias:(BOOL)antiAlias __attribute__((swift_name("clipRect(r:mode:antiAlias:)")));
- (SharedSkikoCanvas *)clipRegionR:(SharedSkikoRegion *)r __attribute__((swift_name("clipRegion(r:)")));
- (SharedSkikoCanvas *)clipRegionR:(SharedSkikoRegion *)r mode:(SharedSkikoClipMode *)mode __attribute__((swift_name("clipRegion(r:mode:)")));
- (SharedSkikoCanvas *)concatMatrix:(SharedSkikoMatrix33 *)matrix __attribute__((swift_name("concat(matrix:)")));
- (SharedSkikoCanvas *)concatMatrix_:(SharedSkikoMatrix44 *)matrix __attribute__((swift_name("concat(matrix_:)")));
- (SharedSkikoCanvas *)drawArcLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom startAngle:(float)startAngle sweepAngle:(float)sweepAngle includeCenter:(BOOL)includeCenter paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawArc(left:top:right:bottom:startAngle:sweepAngle:includeCenter:paint:)")));
- (SharedSkikoCanvas *)drawCircleX:(float)x y:(float)y radius:(float)radius paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawCircle(x:y:radius:paint:)")));
- (SharedSkikoCanvas *)drawDRRectOuter:(SharedSkikoRRect *)outer inner:(SharedSkikoRRect *)inner paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawDRRect(outer:inner:paint:)")));
- (SharedSkikoCanvas *)drawDrawableDrawable:(SharedSkikoDrawable *)drawable __attribute__((swift_name("drawDrawable(drawable:)")));
- (SharedSkikoCanvas *)drawDrawableDrawable:(SharedSkikoDrawable *)drawable matrix:(SharedSkikoMatrix33 * _Nullable)matrix __attribute__((swift_name("drawDrawable(drawable:matrix:)")));
- (SharedSkikoCanvas *)drawDrawableDrawable:(SharedSkikoDrawable *)drawable x:(float)x y:(float)y __attribute__((swift_name("drawDrawable(drawable:x:y:)")));
- (SharedSkikoCanvas *)drawImageImage:(SharedSkikoImage *)image left:(float)left top:(float)top __attribute__((swift_name("drawImage(image:left:top:)")));
- (SharedSkikoCanvas *)drawImageImage:(SharedSkikoImage *)image left:(float)left top:(float)top paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("drawImage(image:left:top:paint:)")));
- (SharedSkikoCanvas *)drawImageNineImage:(SharedSkikoImage *)image center:(SharedSkikoIRect *)center dst:(SharedSkikoRect *)dst filterMode:(SharedSkikoFilterMode *)filterMode paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("drawImageNine(image:center:dst:filterMode:paint:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image dst:(SharedSkikoRect *)dst __attribute__((swift_name("drawImageRect(image:dst:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image dst:(SharedSkikoRect *)dst paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("drawImageRect(image:dst:paint:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image src:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst __attribute__((swift_name("drawImageRect(image:src:dst:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image src:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("drawImageRect(image:src:dst:paint:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image src:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst paint:(SharedSkikoPaint * _Nullable)paint strict:(BOOL)strict __attribute__((swift_name("drawImageRect(image:src:dst:paint:strict:)")));
- (SharedSkikoCanvas *)drawImageRectImage:(SharedSkikoImage *)image src:(SharedSkikoRect *)src dst:(SharedSkikoRect *)dst samplingMode:(id<SharedSkikoSamplingMode>)samplingMode paint:(SharedSkikoPaint * _Nullable)paint strict:(BOOL)strict __attribute__((swift_name("drawImageRect(image:src:dst:samplingMode:paint:strict:)")));
- (SharedSkikoCanvas *)drawLineX0:(float)x0 y0:(float)y0 x1:(float)x1 y1:(float)y1 paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawLine(x0:y0:x1:y1:paint:)")));
- (SharedSkikoCanvas *)drawLinesCoords:(SharedKotlinArray<SharedSkikoPoint *> *)coords paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawLines(coords:paint:)")));
- (SharedSkikoCanvas *)drawLinesCoords:(SharedKotlinFloatArray *)coords paint_:(SharedSkikoPaint *)paint __attribute__((swift_name("drawLines(coords:paint_:)")));
- (SharedSkikoCanvas *)drawOvalR:(SharedSkikoRect *)r paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawOval(r:paint:)")));
- (SharedSkikoCanvas *)drawPaintPaint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPaint(paint:)")));
- (SharedSkikoCanvas *)drawPatchCubics:(SharedKotlinArray<SharedSkikoPoint *> *)cubics colors:(SharedKotlinIntArray *)colors texCoords:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)texCoords blendMode:(SharedSkikoBlendMode *)blendMode paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPatch(cubics:colors:texCoords:blendMode:paint:)")));
- (SharedSkikoCanvas *)drawPathPath:(SharedSkikoPath *)path paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPath(path:paint:)")));
- (SharedSkikoCanvas *)drawPicturePicture:(SharedSkikoPicture *)picture matrix:(SharedSkikoMatrix33 * _Nullable)matrix paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("drawPicture(picture:matrix:paint:)")));
- (SharedSkikoCanvas *)drawPointX:(float)x y:(float)y paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPoint(x:y:paint:)")));
- (SharedSkikoCanvas *)drawPointsCoords:(SharedKotlinArray<SharedSkikoPoint *> *)coords paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPoints(coords:paint:)")));
- (SharedSkikoCanvas *)drawPointsCoords:(SharedKotlinFloatArray *)coords paint_:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPoints(coords:paint_:)")));
- (SharedSkikoCanvas *)drawPolygonCoords:(SharedKotlinArray<SharedSkikoPoint *> *)coords paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPolygon(coords:paint:)")));
- (SharedSkikoCanvas *)drawPolygonCoords:(SharedKotlinFloatArray *)coords paint_:(SharedSkikoPaint *)paint __attribute__((swift_name("drawPolygon(coords:paint_:)")));
- (SharedSkikoCanvas *)drawRRectR:(SharedSkikoRRect *)r paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawRRect(r:paint:)")));
- (SharedSkikoCanvas *)drawRectR:(SharedSkikoRect *)r paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawRect(r:paint:)")));
- (SharedSkikoCanvas *)drawRectShadowR:(SharedSkikoRect *)r dx:(float)dx dy:(float)dy blur:(float)blur color:(int32_t)color __attribute__((swift_name("drawRectShadow(r:dx:dy:blur:color:)")));
- (SharedSkikoCanvas *)drawRectShadowR:(SharedSkikoRect *)r dx:(float)dx dy:(float)dy blur:(float)blur spread:(float)spread color:(int32_t)color __attribute__((swift_name("drawRectShadow(r:dx:dy:blur:spread:color:)")));
- (SharedSkikoCanvas *)drawRectShadowNoclipR:(SharedSkikoRect *)r dx:(float)dx dy:(float)dy blur:(float)blur spread:(float)spread color:(int32_t)color __attribute__((swift_name("drawRectShadowNoclip(r:dx:dy:blur:spread:color:)")));
- (SharedSkikoCanvas *)drawRegionR:(SharedSkikoRegion *)r paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawRegion(r:paint:)")));
- (SharedSkikoCanvas *)drawStringS:(NSString *)s x:(float)x y:(float)y font:(SharedSkikoFont * _Nullable)font paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawString(s:x:y:font:paint:)")));
- (SharedSkikoCanvas *)drawTextBlobBlob:(SharedSkikoTextBlob *)blob x:(float)x y:(float)y paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawTextBlob(blob:x:y:paint:)")));
- (SharedSkikoCanvas *)drawTextLineLine:(SharedSkikoTextLine *)line x:(float)x y:(float)y paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawTextLine(line:x:y:paint:)")));
- (SharedSkikoCanvas *)drawTriangleFanPositions:(SharedKotlinArray<SharedSkikoPoint *> *)positions colors:(SharedKotlinIntArray * _Nullable)colors texCoords:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)texCoords indices:(SharedKotlinShortArray * _Nullable)indices blendMode:(SharedSkikoBlendMode *)blendMode paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawTriangleFan(positions:colors:texCoords:indices:blendMode:paint:)")));
- (SharedSkikoCanvas *)drawTriangleStripPositions:(SharedKotlinArray<SharedSkikoPoint *> *)positions colors:(SharedKotlinIntArray * _Nullable)colors texCoords:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)texCoords indices:(SharedKotlinShortArray * _Nullable)indices blendMode:(SharedSkikoBlendMode *)blendMode paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawTriangleStrip(positions:colors:texCoords:indices:blendMode:paint:)")));
- (SharedSkikoCanvas *)drawTrianglesPositions:(SharedKotlinArray<SharedSkikoPoint *> *)positions colors:(SharedKotlinIntArray * _Nullable)colors texCoords:(SharedKotlinArray<SharedSkikoPoint *> * _Nullable)texCoords indices:(SharedKotlinShortArray * _Nullable)indices blendMode:(SharedSkikoBlendMode *)blendMode paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawTriangles(positions:colors:texCoords:indices:blendMode:paint:)")));
- (SharedSkikoCanvas *)drawVerticesVertexMode:(SharedSkikoVertexMode *)vertexMode positions:(SharedKotlinFloatArray *)positions colors:(SharedKotlinIntArray * _Nullable)colors texCoords:(SharedKotlinFloatArray * _Nullable)texCoords indices:(SharedKotlinShortArray * _Nullable)indices blendMode:(SharedSkikoBlendMode *)blendMode paint:(SharedSkikoPaint *)paint __attribute__((swift_name("drawVertices(vertexMode:positions:colors:texCoords:indices:blendMode:paint:)")));
- (BOOL)readPixelsBitmap:(SharedSkikoBitmap *)bitmap srcX:(int32_t)srcX srcY:(int32_t)srcY __attribute__((swift_name("readPixels(bitmap:srcX:srcY:)")));
- (SharedSkikoCanvas *)resetMatrix __attribute__((swift_name("resetMatrix()")));
- (SharedSkikoCanvas *)restore __attribute__((swift_name("restore()")));
- (SharedSkikoCanvas *)restoreToCountSaveCount:(int32_t)saveCount __attribute__((swift_name("restoreToCount(saveCount:)")));
- (SharedSkikoCanvas *)rotateDeg:(float)deg __attribute__((swift_name("rotate(deg:)")));
- (SharedSkikoCanvas *)rotateDeg:(float)deg x:(float)x y:(float)y __attribute__((swift_name("rotate(deg:x:y:)")));
- (int32_t)save __attribute__((swift_name("save()")));
- (int32_t)saveLayerLayerRec:(SharedSkikoCanvasSaveLayerRec *)layerRec __attribute__((swift_name("saveLayer(layerRec:)")));
- (int32_t)saveLayerBounds:(SharedSkikoRect * _Nullable)bounds paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("saveLayer(bounds:paint:)")));
- (int32_t)saveLayerLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("saveLayer(left:top:right:bottom:paint:)")));
- (SharedSkikoCanvas *)scaleSx:(float)sx sy:(float)sy __attribute__((swift_name("scale(sx:sy:)")));
- (SharedSkikoCanvas *)setMatrixMatrix:(SharedSkikoMatrix33 *)matrix __attribute__((swift_name("setMatrix(matrix:)")));
- (SharedSkikoCanvas *)skewSx:(float)sx sy:(float)sy __attribute__((swift_name("skew(sx:sy:)")));
- (SharedSkikoCanvas *)translateDx:(float)dx dy:(float)dy __attribute__((swift_name("translate(dx:dy:)")));
- (BOOL)writePixelsBitmap:(SharedSkikoBitmap *)bitmap x:(int32_t)x y:(int32_t)y __attribute__((swift_name("writePixels(bitmap:x:y:)")));
@property (readonly) SharedSkikoMatrix44 *localToDevice __attribute__((swift_name("localToDevice")));
@property (readonly) SharedSkikoMatrix33 *localToDeviceAsMatrix33 __attribute__((swift_name("localToDeviceAsMatrix33")));
@property (readonly) int32_t saveCount __attribute__((swift_name("saveCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoContentChangeMode")))
@interface SharedSkikoContentChangeMode : SharedKotlinEnum<SharedSkikoContentChangeMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoContentChangeMode *discard __attribute__((swift_name("discard")));
@property (class, readonly) SharedSkikoContentChangeMode *retain_ __attribute__((swift_name("retain_")));
+ (SharedKotlinArray<SharedSkikoContentChangeMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoContentChangeMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoBackendRenderTarget")))
@interface SharedSkikoBackendRenderTarget : SharedSkikoManaged
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoBackendRenderTargetCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoSurfaceOrigin")))
@interface SharedSkikoSurfaceOrigin : SharedKotlinEnum<SharedSkikoSurfaceOrigin *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoSurfaceOrigin *topLeft __attribute__((swift_name("topLeft")));
@property (class, readonly) SharedSkikoSurfaceOrigin *bottomLeft __attribute__((swift_name("bottomLeft")));
+ (SharedKotlinArray<SharedSkikoSurfaceOrigin *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoSurfaceOrigin *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoSurfaceColorFormat")))
@interface SharedSkikoSurfaceColorFormat : SharedKotlinEnum<SharedSkikoSurfaceColorFormat *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoSurfaceColorFormat *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *alpha8 __attribute__((swift_name("alpha8")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgb565 __attribute__((swift_name("rgb565")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *argb4444 __attribute__((swift_name("argb4444")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgba8888 __attribute__((swift_name("rgba8888")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgb888x __attribute__((swift_name("rgb888x")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *bgra8888 __attribute__((swift_name("bgra8888")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgba1010102 __attribute__((swift_name("rgba1010102")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgb101010x __attribute__((swift_name("rgb101010x")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *gray8 __attribute__((swift_name("gray8")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgbaF16Norm __attribute__((swift_name("rgbaF16Norm")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgbaF16 __attribute__((swift_name("rgbaF16")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *rgbaF32 __attribute__((swift_name("rgbaF32")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *r8g8Unorm __attribute__((swift_name("r8g8Unorm")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *a16Float __attribute__((swift_name("a16Float")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *r16g16Float __attribute__((swift_name("r16g16Float")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *a16Unorm __attribute__((swift_name("a16Unorm")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *r16g16Unorm __attribute__((swift_name("r16g16Unorm")));
@property (class, readonly) SharedSkikoSurfaceColorFormat *r16g16b16a16Unorm __attribute__((swift_name("r16g16b16a16Unorm")));
+ (SharedKotlinArray<SharedSkikoSurfaceColorFormat *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoSurfaceColorFormat *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoSurfaceProps")))
@interface SharedSkikoSurfaceProps : SharedBase
- (instancetype)initWithGeo:(SharedSkikoPixelGeometry *)geo __attribute__((swift_name("init(geo:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithIsDeviceIndependentFonts:(BOOL)isDeviceIndependentFonts pixelGeometry:(SharedSkikoPixelGeometry *)pixelGeometry __attribute__((swift_name("init(isDeviceIndependentFonts:pixelGeometry:)"))) __attribute__((objc_designated_initializer));
- (int32_t)_getFlags __attribute__((swift_name("_getFlags()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoSurfaceProps *)withDeviceIndependentFonts_deviceIndependentFonts:(BOOL)_deviceIndependentFonts __attribute__((swift_name("withDeviceIndependentFonts(_deviceIndependentFonts:)")));
- (SharedSkikoSurfaceProps *)withPixelGeometry_pixelGeometry:(SharedSkikoPixelGeometry *)_pixelGeometry __attribute__((swift_name("withPixelGeometry(_pixelGeometry:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoCanvas.Companion")))
@interface SharedSkikoCanvasCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoCanvasCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoClipMode")))
@interface SharedSkikoClipMode : SharedKotlinEnum<SharedSkikoClipMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoClipMode *difference __attribute__((swift_name("difference")));
@property (class, readonly) SharedSkikoClipMode *intersect __attribute__((swift_name("intersect")));
+ (SharedKotlinArray<SharedSkikoClipMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoClipMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRegion")))
@interface SharedSkikoRegion : SharedSkikoManaged
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoRegionCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)computeRegionComplexity __attribute__((swift_name("computeRegionComplexity()")));
- (BOOL)containsRect:(SharedSkikoIRect *)rect __attribute__((swift_name("contains(rect:)")));
- (BOOL)containsR:(SharedSkikoRegion * _Nullable)r __attribute__((swift_name("contains(r:)")));
- (BOOL)containsX:(int32_t)x y:(int32_t)y __attribute__((swift_name("contains(x:y:)")));
- (BOOL)getBoundaryPathP:(SharedSkikoPath * _Nullable)p __attribute__((swift_name("getBoundaryPath(p:)")));
- (BOOL)intersectsRect:(SharedSkikoIRect *)rect __attribute__((swift_name("intersects(rect:)")));
- (BOOL)intersectsR:(SharedSkikoRegion * _Nullable)r __attribute__((swift_name("intersects(r:)")));
- (BOOL)opRect:(SharedSkikoIRect *)rect op:(SharedSkikoRegionOp *)op __attribute__((swift_name("op(rect:op:)")));
- (BOOL)opR:(SharedSkikoRegion * _Nullable)r op:(SharedSkikoRegionOp *)op __attribute__((swift_name("op(r:op:)")));
- (BOOL)opRect:(SharedSkikoIRect *)rect r:(SharedSkikoRegion * _Nullable)r op:(SharedSkikoRegionOp *)op __attribute__((swift_name("op(rect:r:op:)")));
- (BOOL)opR:(SharedSkikoRegion * _Nullable)r rect:(SharedSkikoIRect *)rect op:(SharedSkikoRegionOp *)op __attribute__((swift_name("op(r:rect:op:)")));
- (BOOL)opA:(SharedSkikoRegion * _Nullable)a b:(SharedSkikoRegion * _Nullable)b op:(SharedSkikoRegionOp *)op __attribute__((swift_name("op(a:b:op:)")));
- (BOOL)quickContainsRect:(SharedSkikoIRect *)rect __attribute__((swift_name("quickContains(rect:)")));
- (BOOL)quickRejectRect:(SharedSkikoIRect *)rect __attribute__((swift_name("quickReject(rect:)")));
- (BOOL)quickRejectR:(SharedSkikoRegion * _Nullable)r __attribute__((swift_name("quickReject(r:)")));
- (BOOL)setR:(SharedSkikoRegion * _Nullable)r __attribute__((swift_name("set(r:)")));
- (BOOL)setEmpty __attribute__((swift_name("setEmpty()")));
- (BOOL)setPathPath:(SharedSkikoPath * _Nullable)path clip:(SharedSkikoRegion * _Nullable)clip __attribute__((swift_name("setPath(path:clip:)")));
- (BOOL)setRectRect:(SharedSkikoIRect *)rect __attribute__((swift_name("setRect(rect:)")));
- (BOOL)setRectsRects:(SharedKotlinArray<SharedSkikoIRect *> *)rects __attribute__((swift_name("setRects(rects:)")));
- (BOOL)setRegionR:(SharedSkikoRegion * _Nullable)r __attribute__((swift_name("setRegion(r:)")));
- (void)translateDx:(int32_t)dx dy:(int32_t)dy __attribute__((swift_name("translate(dx:dy:)")));
@property (readonly) SharedSkikoIRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) BOOL isComplex __attribute__((swift_name("isComplex")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isRect __attribute__((swift_name("isRect")));
@end

__attribute__((swift_name("SkikoDrawable")))
@interface SharedSkikoDrawable : SharedSkikoManaged
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoDrawableCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoDrawable *)drawCanvas:(SharedSkikoCanvas * _Nullable)canvas __attribute__((swift_name("draw(canvas:)")));
- (SharedSkikoDrawable *)drawCanvas:(SharedSkikoCanvas * _Nullable)canvas matrix:(SharedSkikoMatrix33 * _Nullable)matrix __attribute__((swift_name("draw(canvas:matrix:)")));
- (SharedSkikoDrawable *)drawCanvas:(SharedSkikoCanvas * _Nullable)canvas x:(float)x y:(float)y __attribute__((swift_name("draw(canvas:x:y:)")));
- (SharedSkikoPicture *)makePictureSnapshot __attribute__((swift_name("makePictureSnapshot()")));
- (SharedSkikoDrawable *)notifyDrawingChanged __attribute__((swift_name("notifyDrawingChanged()")));
- (void)onDrawCanvas:(SharedSkikoCanvas * _Nullable)canvas __attribute__((swift_name("onDraw(canvas:)")));
- (SharedSkikoRect *)onGetBounds __attribute__((swift_name("onGetBounds()")));
@property (readonly) SharedSkikoRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) int32_t generationId __attribute__((swift_name("generationId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFilterMode")))
@interface SharedSkikoFilterMode : SharedKotlinEnum<SharedSkikoFilterMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFilterMode *nearest __attribute__((swift_name("nearest")));
@property (class, readonly) SharedSkikoFilterMode *linear __attribute__((swift_name("linear")));
+ (SharedKotlinArray<SharedSkikoFilterMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFilterMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPicture")))
@interface SharedSkikoPicture : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoPictureCompanion *companion __attribute__((swift_name("companion")));
- (SharedSkikoShader *)makeShaderTmx:(SharedSkikoFilterTileMode *)tmx tmy:(SharedSkikoFilterTileMode *)tmy mode:(SharedSkikoFilterMode *)mode localMatrix:(SharedSkikoMatrix33 * _Nullable)localMatrix tileRect:(SharedSkikoRect * _Nullable)tileRect __attribute__((swift_name("makeShader(tmx:tmy:mode:localMatrix:tileRect:)")));
- (SharedSkikoPicture *)playbackCanvas:(SharedSkikoCanvas * _Nullable)canvas abort:(SharedBoolean *(^ _Nullable)(void))abort __attribute__((swift_name("playback(canvas:abort:)")));
- (SharedSkikoData *)serializeToData __attribute__((swift_name("serializeToData()")));
@property (readonly) void * _Nullable approximateBytesUsed __attribute__((swift_name("approximateBytesUsed")));
@property (readonly) int32_t approximateOpCount __attribute__((swift_name("approximateOpCount")));
@property (readonly) SharedSkikoRect *cullRect __attribute__((swift_name("cullRect")));
@property (readonly) int32_t uniqueId __attribute__((swift_name("uniqueId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFont")))
@interface SharedSkikoFont : SharedSkikoManaged
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithTypeface:(SharedSkikoTypeface * _Nullable)typeface __attribute__((swift_name("init(typeface:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithTypeface:(SharedSkikoTypeface * _Nullable)typeface size:(float)size __attribute__((swift_name("init(typeface:size:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithTypeface:(SharedSkikoTypeface * _Nullable)typeface size:(float)size scaleX:(float)scaleX skewX:(float)skewX __attribute__((swift_name("init(typeface:size:scaleX:skewX:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoFontCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)areBitmapsEmbedded __attribute__((swift_name("areBitmapsEmbedded()")));
- (SharedKotlinArray<SharedSkikoRect *> *)getBoundsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getBounds(glyphs:)")));
- (SharedKotlinArray<SharedSkikoRect *> *)getBoundsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs p:(SharedSkikoPaint * _Nullable)p __attribute__((swift_name("getBounds(glyphs:p:)")));
- (SharedSkikoPath * _Nullable)getPathGlyph:(int16_t)glyph __attribute__((swift_name("getPath(glyph:)")));
- (SharedKotlinArray<SharedSkikoPath *> *)getPathsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getPaths(glyphs:)")));
- (SharedKotlinArray<SharedSkikoPoint *> *)getPositionsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getPositions(glyphs:)")));
- (SharedKotlinArray<SharedSkikoPoint *> *)getPositionsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs offset:(SharedSkikoPoint *)offset __attribute__((swift_name("getPositions(glyphs:offset:)")));
- (SharedKotlinShortArray *)getStringGlyphsS:(NSString *)s __attribute__((swift_name("getStringGlyphs(s:)")));
- (int32_t)getStringGlyphsCountS:(NSString * _Nullable)s __attribute__((swift_name("getStringGlyphsCount(s:)")));
- (int16_t)getUTF32GlyphUnichar:(int32_t)unichar __attribute__((swift_name("getUTF32Glyph(unichar:)")));
- (SharedKotlinShortArray *)getUTF32GlyphsUni:(SharedKotlinIntArray * _Nullable)uni __attribute__((swift_name("getUTF32Glyphs(uni:)")));
- (SharedKotlinFloatArray *)getWidthsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getWidths(glyphs:)")));
- (SharedKotlinFloatArray *)getXPositionsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getXPositions(glyphs:)")));
- (SharedKotlinFloatArray *)getXPositionsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs offset:(float)offset __attribute__((swift_name("getXPositions(glyphs:offset:)")));
- (SharedSkikoFont *)makeWithSizeSize:(float)size __attribute__((swift_name("makeWithSize(size:)")));
- (SharedSkikoRect *)measureTextS:(NSString * _Nullable)s p:(SharedSkikoPaint * _Nullable)p __attribute__((swift_name("measureText(s:p:)")));
- (float)measureTextWidthS:(NSString * _Nullable)s __attribute__((swift_name("measureTextWidth(s:)")));
- (float)measureTextWidthS:(NSString * _Nullable)s p:(SharedSkikoPaint * _Nullable)p __attribute__((swift_name("measureTextWidth(s:p:)")));
- (void)setBitmapsEmbeddedValue:(BOOL)value __attribute__((swift_name("setBitmapsEmbedded(value:)")));
- (SharedSkikoFont *)setTypefaceTypeface:(SharedSkikoTypeface * _Nullable)typeface __attribute__((swift_name("setTypeface(typeface:)")));
@property SharedSkikoFontEdging *edging __attribute__((swift_name("edging")));
@property SharedSkikoFontHinting *hinting __attribute__((swift_name("hinting")));
@property BOOL isAutoHintingForced __attribute__((swift_name("isAutoHintingForced")));
@property BOOL isBaselineSnapped __attribute__((swift_name("isBaselineSnapped")));
@property BOOL isEmboldened __attribute__((swift_name("isEmboldened")));
@property BOOL isLinearMetrics __attribute__((swift_name("isLinearMetrics")));
@property BOOL isSubpixel __attribute__((swift_name("isSubpixel")));
@property (readonly) SharedSkikoFontMetrics *metrics __attribute__((swift_name("metrics")));
@property float scaleX __attribute__((swift_name("scaleX")));
@property float size __attribute__((swift_name("size")));
@property float skewX __attribute__((swift_name("skewX")));
@property (readonly) float spacing __attribute__((swift_name("spacing")));
@property (readonly) SharedSkikoTypeface * _Nullable typeface __attribute__((swift_name("typeface")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTextBlob")))
@interface SharedSkikoTextBlob : SharedSkikoManaged <SharedKotlinIterable>
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoTextBlobCompanion *companion __attribute__((swift_name("companion")));
- (SharedKotlinFloatArray * _Nullable)getInterceptsLowerBound:(float)lowerBound upperBound:(float)upperBound __attribute__((swift_name("getIntercepts(lowerBound:upperBound:)")));
- (SharedKotlinFloatArray * _Nullable)getInterceptsLowerBound:(float)lowerBound upperBound:(float)upperBound paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("getIntercepts(lowerBound:upperBound:paint:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (SharedSkikoData *)serializeToData __attribute__((swift_name("serializeToData()")));
@property (readonly) SharedSkikoRect *blockBounds __attribute__((swift_name("blockBounds")));
@property (readonly) SharedSkikoRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) SharedKotlinIntArray *clusters __attribute__((swift_name("clusters")));
@property (readonly) float firstBaseline __attribute__((swift_name("firstBaseline")));
@property (readonly) SharedKotlinShortArray *glyphs __attribute__((swift_name("glyphs")));
@property (readonly) float lastBaseline __attribute__((swift_name("lastBaseline")));
@property (readonly) SharedKotlinFloatArray *positions __attribute__((swift_name("positions")));
@property (readonly) SharedSkikoRect *tightBounds __attribute__((swift_name("tightBounds")));
@property (readonly) int32_t uniqueId __attribute__((swift_name("uniqueId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTextLine")))
@interface SharedSkikoTextLine : SharedSkikoManaged
- (instancetype)initWithPtr:(void * _Nullable)ptr finalizer:(void * _Nullable)finalizer managed:(BOOL)managed __attribute__((swift_name("init(ptr:finalizer:managed:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoTextLineCompanion *companion __attribute__((swift_name("companion")));
- (float)getCoordAtOffsetOffset:(int32_t)offset __attribute__((swift_name("getCoordAtOffset(offset:)")));
- (SharedKotlinFloatArray * _Nullable)getInterceptsLowerBound:(float)lowerBound upperBound:(float)upperBound __attribute__((swift_name("getIntercepts(lowerBound:upperBound:)")));
- (SharedKotlinFloatArray * _Nullable)getInterceptsLowerBound:(float)lowerBound upperBound:(float)upperBound paint:(SharedSkikoPaint * _Nullable)paint __attribute__((swift_name("getIntercepts(lowerBound:upperBound:paint:)")));
- (int32_t)getLeftOffsetAtCoordX:(float)x __attribute__((swift_name("getLeftOffsetAtCoord(x:)")));
- (int32_t)getOffsetAtCoordX:(float)x __attribute__((swift_name("getOffsetAtCoord(x:)")));
@property (readonly) float ascent __attribute__((swift_name("ascent")));
@property (readonly) float capHeight __attribute__((swift_name("capHeight")));
@property (readonly) float descent __attribute__((swift_name("descent")));
@property (readonly) SharedKotlinShortArray *glyphs __attribute__((swift_name("glyphs")));
@property (readonly) float height __attribute__((swift_name("height")));
@property (readonly) float leading __attribute__((swift_name("leading")));
@property (readonly) SharedKotlinFloatArray *positions __attribute__((swift_name("positions")));
@property (readonly) SharedSkikoTextBlob * _Nullable textBlob __attribute__((swift_name("textBlob")));
@property (readonly) float width __attribute__((swift_name("width")));
@property (readonly) float xHeight __attribute__((swift_name("xHeight")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinShortArray")))
@interface SharedKotlinShortArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedShort *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int16_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinShortIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int16_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoVertexMode")))
@interface SharedSkikoVertexMode : SharedKotlinEnum<SharedSkikoVertexMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoVertexMode *triangles __attribute__((swift_name("triangles")));
@property (class, readonly) SharedSkikoVertexMode *triangleStrip __attribute__((swift_name("triangleStrip")));
@property (class, readonly) SharedSkikoVertexMode *triangleFan __attribute__((swift_name("triangleFan")));
+ (SharedKotlinArray<SharedSkikoVertexMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoVertexMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoCanvas.SaveLayerRec")))
@interface SharedSkikoCanvasSaveLayerRec : SharedBase
- (instancetype)initWithBounds:(SharedSkikoRect * _Nullable)bounds paint:(SharedSkikoPaint * _Nullable)paint backdrop:(SharedSkikoImageFilter * _Nullable)backdrop colorSpace:(SharedSkikoColorSpace * _Nullable)colorSpace saveLayerFlags:(SharedSkikoCanvasSaveLayerFlags *)saveLayerFlags __attribute__((swift_name("init(bounds:paint:backdrop:colorSpace:saveLayerFlags:)"))) __attribute__((objc_designated_initializer));
@property (readonly) SharedSkikoImageFilter * _Nullable backdrop __attribute__((swift_name("backdrop")));
@property (readonly) SharedSkikoRect * _Nullable bounds __attribute__((swift_name("bounds")));
@property (readonly) SharedSkikoColorSpace * _Nullable colorSpace __attribute__((swift_name("colorSpace")));
@property (readonly) SharedSkikoPaint * _Nullable paint __attribute__((swift_name("paint")));
@property (readonly) SharedSkikoCanvasSaveLayerFlags *saveLayerFlags __attribute__((swift_name("saveLayerFlags")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoBackendRenderTarget.Companion")))
@interface SharedSkikoBackendRenderTargetCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoBackendRenderTargetCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoBackendRenderTarget *)makeDirect3DWidth:(int32_t)width height:(int32_t)height texturePtr:(void * _Nullable)texturePtr format:(int32_t)format sampleCnt:(int32_t)sampleCnt levelCnt:(int32_t)levelCnt __attribute__((swift_name("makeDirect3D(width:height:texturePtr:format:sampleCnt:levelCnt:)")));
- (SharedSkikoBackendRenderTarget *)makeGLWidth:(int32_t)width height:(int32_t)height sampleCnt:(int32_t)sampleCnt stencilBits:(int32_t)stencilBits fbId:(int32_t)fbId fbFormat:(int32_t)fbFormat __attribute__((swift_name("makeGL(width:height:sampleCnt:stencilBits:fbId:fbFormat:)")));
- (SharedSkikoBackendRenderTarget *)makeMetalWidth:(int32_t)width height:(int32_t)height texturePtr:(void * _Nullable)texturePtr __attribute__((swift_name("makeMetal(width:height:texturePtr:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPixelGeometry")))
@interface SharedSkikoPixelGeometry : SharedKotlinEnum<SharedSkikoPixelGeometry *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoPixelGeometry *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedSkikoPixelGeometry *rgbH __attribute__((swift_name("rgbH")));
@property (class, readonly) SharedSkikoPixelGeometry *bgrH __attribute__((swift_name("bgrH")));
@property (class, readonly) SharedSkikoPixelGeometry *rgbV __attribute__((swift_name("rgbV")));
@property (class, readonly) SharedSkikoPixelGeometry *bgrV __attribute__((swift_name("bgrV")));
+ (SharedKotlinArray<SharedSkikoPixelGeometry *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoPixelGeometry *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRegion.Companion")))
@interface SharedSkikoRegionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRegionCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRegion.Op")))
@interface SharedSkikoRegionOp : SharedKotlinEnum<SharedSkikoRegionOp *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoRegionOpCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedSkikoRegionOp *difference __attribute__((swift_name("difference")));
@property (class, readonly) SharedSkikoRegionOp *intersect __attribute__((swift_name("intersect")));
@property (class, readonly) SharedSkikoRegionOp *union_ __attribute__((swift_name("union_")));
@property (class, readonly) SharedSkikoRegionOp *xor_ __attribute__((swift_name("xor_")));
@property (class, readonly) SharedSkikoRegionOp *reverseDifference __attribute__((swift_name("reverseDifference")));
@property (class, readonly) SharedSkikoRegionOp *replace __attribute__((swift_name("replace")));
+ (SharedKotlinArray<SharedSkikoRegionOp *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoRegionOp *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoDrawable.Companion")))
@interface SharedSkikoDrawableCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoDrawableCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPicture.Companion")))
@interface SharedSkikoPictureCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoPictureCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoPicture * _Nullable)makeFromDataData:(SharedSkikoData * _Nullable)data __attribute__((swift_name("makeFromData(data:)")));
- (SharedSkikoPicture *)makePlaceholderCull:(SharedSkikoRect *)cull __attribute__((swift_name("makePlaceholder(cull:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTypeface")))
@interface SharedSkikoTypeface : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoTypefaceCompanion *companion __attribute__((swift_name("companion")));
- (SharedKotlinIntArray * _Nullable)getKerningPairAdjustmentsGlyphs:(SharedKotlinShortArray * _Nullable)glyphs __attribute__((swift_name("getKerningPairAdjustments(glyphs:)")));
- (SharedKotlinShortArray *)getStringGlyphsS:(NSString *)s __attribute__((swift_name("getStringGlyphs(s:)")));
- (SharedSkikoData * _Nullable)getTableDataTag:(NSString *)tag __attribute__((swift_name("getTableData(tag:)")));
- (void * _Nullable)getTableSizeTag:(NSString *)tag __attribute__((swift_name("getTableSize(tag:)")));
- (int16_t)getUTF32GlyphUnichar:(int32_t)unichar __attribute__((swift_name("getUTF32Glyph(unichar:)")));
- (SharedKotlinShortArray *)getUTF32GlyphsUni:(SharedKotlinIntArray * _Nullable)uni __attribute__((swift_name("getUTF32Glyphs(uni:)")));
- (SharedSkikoTypeface *)makeCloneVariation:(SharedSkikoFontVariation *)variation __attribute__((swift_name("makeClone(variation:)")));
- (SharedSkikoTypeface *)makeCloneVariations:(SharedKotlinArray<SharedSkikoFontVariation *> *)variations collectionIndex:(int32_t)collectionIndex __attribute__((swift_name("makeClone(variations:collectionIndex:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedSkikoRect *bounds __attribute__((swift_name("bounds")));
@property (readonly) NSString *familyName __attribute__((swift_name("familyName")));
@property (readonly) SharedKotlinArray<SharedSkikoFontFamilyName *> *familyNames __attribute__((swift_name("familyNames")));
@property (readonly) SharedSkikoFontStyle *fontStyle __attribute__((swift_name("fontStyle")));
@property (readonly) int32_t glyphsCount __attribute__((swift_name("glyphsCount")));
@property (readonly) BOOL isBold __attribute__((swift_name("isBold")));
@property (readonly) BOOL isFixedPitch __attribute__((swift_name("isFixedPitch")));
@property (readonly) BOOL isItalic __attribute__((swift_name("isItalic")));
@property (readonly) SharedKotlinArray<NSString *> *tableTags __attribute__((swift_name("tableTags")));
@property (readonly) int32_t tablesCount __attribute__((swift_name("tablesCount")));
@property (readonly) int32_t uniqueId __attribute__((swift_name("uniqueId")));
@property (readonly) int32_t unitsPerEm __attribute__((swift_name("unitsPerEm")));
@property (readonly) SharedKotlinArray<SharedSkikoFontVariationAxis *> * _Nullable variationAxes __attribute__((swift_name("variationAxes")));
@property (readonly) SharedKotlinArray<SharedSkikoFontVariation *> * _Nullable variations __attribute__((swift_name("variations")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFont.Companion")))
@interface SharedSkikoFontCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontEdging")))
@interface SharedSkikoFontEdging : SharedKotlinEnum<SharedSkikoFontEdging *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFontEdging *alias __attribute__((swift_name("alias")));
@property (class, readonly) SharedSkikoFontEdging *antiAlias __attribute__((swift_name("antiAlias")));
@property (class, readonly) SharedSkikoFontEdging *subpixelAntiAlias __attribute__((swift_name("subpixelAntiAlias")));
+ (SharedKotlinArray<SharedSkikoFontEdging *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFontEdging *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontHinting")))
@interface SharedSkikoFontHinting : SharedKotlinEnum<SharedSkikoFontHinting *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFontHinting *none __attribute__((swift_name("none")));
@property (class, readonly) SharedSkikoFontHinting *slight __attribute__((swift_name("slight")));
@property (class, readonly) SharedSkikoFontHinting *normal __attribute__((swift_name("normal")));
@property (class, readonly) SharedSkikoFontHinting *full __attribute__((swift_name("full")));
+ (SharedKotlinArray<SharedSkikoFontHinting *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFontHinting *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontMetrics")))
@interface SharedSkikoFontMetrics : SharedBase
- (instancetype)initWithTop:(float)top ascent:(float)ascent descent:(float)descent bottom:(float)bottom leading:(float)leading avgCharWidth:(float)avgCharWidth maxCharWidth:(float)maxCharWidth xMin:(float)xMin xMax:(float)xMax xHeight:(float)xHeight capHeight:(float)capHeight underlineThickness:(SharedFloat * _Nullable)underlineThickness underlinePosition:(SharedFloat * _Nullable)underlinePosition strikeoutThickness:(SharedFloat * _Nullable)strikeoutThickness strikeoutPosition:(SharedFloat * _Nullable)strikeoutPosition __attribute__((swift_name("init(top:ascent:descent:bottom:leading:avgCharWidth:maxCharWidth:xMin:xMax:xHeight:capHeight:underlineThickness:underlinePosition:strikeoutThickness:strikeoutPosition:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoFontMetricsCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float ascent __attribute__((swift_name("ascent")));
@property (readonly) float avgCharWidth __attribute__((swift_name("avgCharWidth")));
@property (readonly) float bottom __attribute__((swift_name("bottom")));
@property (readonly) float capHeight __attribute__((swift_name("capHeight")));
@property (readonly) float descent __attribute__((swift_name("descent")));
@property (readonly) float height __attribute__((swift_name("height")));
@property (readonly) float leading __attribute__((swift_name("leading")));
@property (readonly) float maxCharWidth __attribute__((swift_name("maxCharWidth")));
@property (readonly) SharedFloat * _Nullable strikeoutPosition __attribute__((swift_name("strikeoutPosition")));
@property (readonly) SharedFloat * _Nullable strikeoutThickness __attribute__((swift_name("strikeoutThickness")));
@property (readonly) float top __attribute__((swift_name("top")));
@property (readonly) SharedFloat * _Nullable underlinePosition __attribute__((swift_name("underlinePosition")));
@property (readonly) SharedFloat * _Nullable underlineThickness __attribute__((swift_name("underlineThickness")));
@property (readonly) float xHeight __attribute__((swift_name("xHeight")));
@property (readonly) float xMax __attribute__((swift_name("xMax")));
@property (readonly) float xMin __attribute__((swift_name("xMin")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTextBlob.Companion")))
@interface SharedSkikoTextBlobCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoTextBlobCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoTextBlob * _Nullable)makeFromDataData:(SharedSkikoData * _Nullable)data __attribute__((swift_name("makeFromData(data:)")));
- (SharedSkikoTextBlob * _Nullable)makeFromPosGlyphs:(SharedKotlinShortArray *)glyphs pos:(SharedKotlinArray<SharedSkikoPoint *> *)pos font:(SharedSkikoFont * _Nullable)font __attribute__((swift_name("makeFromPos(glyphs:pos:font:)")));
- (SharedSkikoTextBlob * _Nullable)makeFromPosHGlyphs:(SharedKotlinShortArray *)glyphs xpos:(SharedKotlinFloatArray *)xpos ypos:(float)ypos font:(SharedSkikoFont * _Nullable)font __attribute__((swift_name("makeFromPosH(glyphs:xpos:ypos:font:)")));
- (SharedSkikoTextBlob * _Nullable)makeFromRSXformGlyphs:(SharedKotlinShortArray *)glyphs xform:(SharedKotlinArray<SharedSkikoRSXform *> *)xform font:(SharedSkikoFont * _Nullable)font __attribute__((swift_name("makeFromRSXform(glyphs:xform:font:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTextLine.Companion")))
@interface SharedSkikoTextLineCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoTextLineCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoTextLine *)makeText:(NSString * _Nullable)text font:(SharedSkikoFont * _Nullable)font __attribute__((swift_name("make(text:font:)")));
- (SharedSkikoTextLine *)makeText:(NSString * _Nullable)text font:(SharedSkikoFont * _Nullable)font opts:(SharedSkikoShapingOptions * _Nullable)opts __attribute__((swift_name("make(text:font:opts:)")));
@end

__attribute__((swift_name("KotlinShortIterator")))
@interface SharedKotlinShortIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedShort *)next __attribute__((swift_name("next()")));
- (int16_t)nextShort __attribute__((swift_name("nextShort()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoCanvas.SaveLayerFlags")))
@interface SharedSkikoCanvasSaveLayerFlags : SharedBase
- (instancetype)initWithFlagsSet:(SharedKotlinArray<SharedSkikoCanvasSaveLayerFlagsSet *> *)flagsSet __attribute__((swift_name("init(flagsSet:)"))) __attribute__((objc_designated_initializer));
- (BOOL)containsFlag:(SharedSkikoCanvasSaveLayerFlagsSet *)flag __attribute__((swift_name("contains(flag:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRegion.OpCompanion")))
@interface SharedSkikoRegionOpCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRegionOpCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoTypeface.Companion")))
@interface SharedSkikoTypefaceCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoTypefaceCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoTypeface *)makeEmpty __attribute__((swift_name("makeEmpty()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontVariation")))
@interface SharedSkikoFontVariation : SharedBase
- (instancetype)initWith_tag:(int32_t)_tag value:(float)value __attribute__((swift_name("init(_tag:value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFeature:(NSString *)feature value:(float)value __attribute__((swift_name("init(feature:value:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoFontVariationCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t _tag __attribute__((swift_name("_tag")));
@property (readonly) NSString *tag __attribute__((swift_name("tag")));
@property (readonly) float value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontFamilyName")))
@interface SharedSkikoFontFamilyName : SharedBase
- (instancetype)initWithName:(NSString *)name language:(NSString *)language __attribute__((swift_name("init(name:language:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *language __attribute__((swift_name("language")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontStyle")))
@interface SharedSkikoFontStyle : SharedBase
- (instancetype)initWithWeight:(int32_t)weight width:(int32_t)width slant:(SharedSkikoFontSlant *)slant __attribute__((swift_name("init(weight:width:slant:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoFontStyleCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoFontStyle *)withSlantSlant:(SharedSkikoFontSlant *)slant __attribute__((swift_name("withSlant(slant:)")));
- (SharedSkikoFontStyle *)withWeightWeight:(int32_t)weight __attribute__((swift_name("withWeight(weight:)")));
- (SharedSkikoFontStyle *)withWidthWidth:(int32_t)width __attribute__((swift_name("withWidth(width:)")));
@property (readonly) int32_t _value __attribute__((swift_name("_value")));
@property (readonly) SharedSkikoFontSlant *slant __attribute__((swift_name("slant")));
@property (readonly) int32_t weight __attribute__((swift_name("weight")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontVariationAxis")))
@interface SharedSkikoFontVariationAxis : SharedBase
- (instancetype)initWithTag:(NSString *)tag min:(float)min def:(float)def max:(float)max __attribute__((swift_name("init(tag:min:def:max:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWith_tag:(int32_t)_tag minValue:(float)minValue defaultValue:(float)defaultValue maxValue:(float)maxValue isHidden:(BOOL)isHidden __attribute__((swift_name("init(_tag:minValue:defaultValue:maxValue:isHidden:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithTag:(NSString *)tag min:(float)min def:(float)def max:(float)max hidden:(BOOL)hidden __attribute__((swift_name("init(tag:min:def:max:hidden:)"))) __attribute__((objc_designated_initializer));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t _tag __attribute__((swift_name("_tag")));
@property (readonly) float defaultValue __attribute__((swift_name("defaultValue")));
@property (readonly) BOOL isHidden __attribute__((swift_name("isHidden")));
@property (readonly) float maxValue __attribute__((swift_name("maxValue")));
@property (readonly) float minValue __attribute__((swift_name("minValue")));
@property (readonly) NSString *tag __attribute__((swift_name("tag")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontMetrics.Companion")))
@interface SharedSkikoFontMetricsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontMetricsCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRSXform")))
@interface SharedSkikoRSXform : SharedBase
- (instancetype)initWithScos:(float)scos ssin:(float)ssin tx:(float)tx ty:(float)ty __attribute__((swift_name("init(scos:ssin:tx:ty:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoRSXformCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoShapingOptions")))
@interface SharedSkikoShapingOptions : SharedBase
- (instancetype)initWithFontMgr:(SharedSkikoFontMgr * _Nullable)fontMgr features:(SharedKotlinArray<SharedSkikoFontFeature *> * _Nullable)features isLeftToRight:(BOOL)isLeftToRight isApproximateSpaces:(BOOL)isApproximateSpaces isApproximatePunctuation:(BOOL)isApproximatePunctuation __attribute__((swift_name("init(fontMgr:features:isLeftToRight:isApproximateSpaces:isApproximatePunctuation:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoShapingOptionsCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (SharedSkikoShapingOptions *)withApproximatePunctuation_approximatePunctuation:(BOOL)_approximatePunctuation __attribute__((swift_name("withApproximatePunctuation(_approximatePunctuation:)")));
- (SharedSkikoShapingOptions *)withApproximateSpaces_approximateSpaces:(BOOL)_approximateSpaces __attribute__((swift_name("withApproximateSpaces(_approximateSpaces:)")));
- (SharedSkikoShapingOptions *)withFeaturesFeatures:(SharedKotlinArray<SharedSkikoFontFeature *> * _Nullable)features __attribute__((swift_name("withFeatures(features:)")));
- (SharedSkikoShapingOptions *)withFeaturesFeaturesString:(NSString * _Nullable)featuresString __attribute__((swift_name("withFeatures(featuresString:)")));
- (SharedSkikoShapingOptions *)withFontMgr_fontMgr:(SharedSkikoFontMgr * _Nullable)_fontMgr __attribute__((swift_name("withFontMgr(_fontMgr:)")));
- (SharedSkikoShapingOptions *)withLeftToRight_leftToRight:(BOOL)_leftToRight __attribute__((swift_name("withLeftToRight(_leftToRight:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoCanvas.SaveLayerFlagsSet")))
@interface SharedSkikoCanvasSaveLayerFlagsSet : SharedKotlinEnum<SharedSkikoCanvasSaveLayerFlagsSet *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoCanvasSaveLayerFlagsSet *preservelcdtext __attribute__((swift_name("preservelcdtext")));
@property (class, readonly) SharedSkikoCanvasSaveLayerFlagsSet *initwithprevious __attribute__((swift_name("initwithprevious")));
@property (class, readonly) SharedSkikoCanvasSaveLayerFlagsSet *f16colortype __attribute__((swift_name("f16colortype")));
+ (SharedKotlinArray<SharedSkikoCanvasSaveLayerFlagsSet *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoCanvasSaveLayerFlagsSet *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t mask __attribute__((swift_name("mask")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontVariation.Companion")))
@interface SharedSkikoFontVariationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontVariationCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinArray<SharedSkikoFontVariation *> *)parseStr:(NSString *)str __attribute__((swift_name("parse(str:)")));
- (SharedSkikoFontVariation *)parseOneS:(NSString *)s __attribute__((swift_name("parseOne(s:)")));
@property (readonly) SharedKotlinArray<SharedSkikoFontVariation *> *EMPTY __attribute__((swift_name("EMPTY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontSlant")))
@interface SharedSkikoFontSlant : SharedKotlinEnum<SharedSkikoFontSlant *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSkikoFontSlant *upright __attribute__((swift_name("upright")));
@property (class, readonly) SharedSkikoFontSlant *italic __attribute__((swift_name("italic")));
@property (class, readonly) SharedSkikoFontSlant *oblique __attribute__((swift_name("oblique")));
+ (SharedKotlinArray<SharedSkikoFontSlant *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSkikoFontSlant *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontStyle.Companion")))
@interface SharedSkikoFontStyleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontStyleCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoFontStyle *BOLD __attribute__((swift_name("BOLD")));
@property (readonly) SharedSkikoFontStyle *BOLD_ITALIC __attribute__((swift_name("BOLD_ITALIC")));
@property (readonly) SharedSkikoFontStyle *ITALIC __attribute__((swift_name("ITALIC")));
@property (readonly) SharedSkikoFontStyle *NORMAL __attribute__((swift_name("NORMAL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoRSXform.Companion")))
@interface SharedSkikoRSXformCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoRSXformCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoRSXform *)makeFromRadiansScale:(float)scale radians:(float)radians tx:(float)tx ty:(float)ty ax:(float)ax ay:(float)ay __attribute__((swift_name("makeFromRadians(scale:radians:tx:ty:ax:ay:)")));
@end

__attribute__((swift_name("SkikoFontMgr")))
@interface SharedSkikoFontMgr : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoFontMgrCompanion *companion __attribute__((swift_name("companion")));
- (NSString *)getFamilyNameIndex:(int32_t)index __attribute__((swift_name("getFamilyName(index:)")));
- (SharedSkikoTypeface * _Nullable)legacyMakeTypefaceName:(NSString *)name style:(SharedSkikoFontStyle *)style __attribute__((swift_name("legacyMakeTypeface(name:style:)")));
- (SharedSkikoTypeface * _Nullable)makeFromDataData:(SharedSkikoData * _Nullable)data ttcIndex:(int32_t)ttcIndex __attribute__((swift_name("makeFromData(data:ttcIndex:)")));
- (SharedSkikoTypeface * _Nullable)makeFromFilePath:(NSString *)path ttcIndex:(int32_t)ttcIndex __attribute__((swift_name("makeFromFile(path:ttcIndex:)")));
- (SharedSkikoFontStyleSet * _Nullable)makeStyleSetIndex:(int32_t)index __attribute__((swift_name("makeStyleSet(index:)")));
- (SharedSkikoTypeface * _Nullable)matchFamiliesStyleFamilies:(SharedKotlinArray<NSString *> *)families style:(SharedSkikoFontStyle *)style __attribute__((swift_name("matchFamiliesStyle(families:style:)")));
- (SharedSkikoTypeface * _Nullable)matchFamiliesStyleCharacterFamilies:(SharedKotlinArray<NSString *> *)families style:(SharedSkikoFontStyle *)style bcp47:(SharedKotlinArray<NSString *> * _Nullable)bcp47 character:(int32_t)character __attribute__((swift_name("matchFamiliesStyleCharacter(families:style:bcp47:character:)")));
- (SharedSkikoFontStyleSet *)matchFamilyFamilyName:(NSString * _Nullable)familyName __attribute__((swift_name("matchFamily(familyName:)")));
- (SharedSkikoTypeface * _Nullable)matchFamilyStyleFamilyName:(NSString * _Nullable)familyName style:(SharedSkikoFontStyle *)style __attribute__((swift_name("matchFamilyStyle(familyName:style:)")));
- (SharedSkikoTypeface * _Nullable)matchFamilyStyleCharacterFamilyName:(NSString * _Nullable)familyName style:(SharedSkikoFontStyle *)style bcp47:(SharedKotlinArray<NSString *> * _Nullable)bcp47 character:(int32_t)character __attribute__((swift_name("matchFamilyStyleCharacter(familyName:style:bcp47:character:)")));
@property (readonly) int32_t familiesCount __attribute__((swift_name("familiesCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontFeature")))
@interface SharedSkikoFontFeature : SharedBase
- (instancetype)initWithFeature:(NSString *)feature __attribute__((swift_name("init(feature:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFeature:(NSString *)feature value:(BOOL)value __attribute__((swift_name("init(feature:value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFeature:(NSString *)feature value_:(int32_t)value __attribute__((swift_name("init(feature:value_:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWith_tag:(int32_t)_tag value:(int32_t)value start:(uint32_t)start end:(uint32_t)end __attribute__((swift_name("init(_tag:value:start:end:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFeature:(NSString *)feature value:(int32_t)value start:(uint32_t)start end:(uint32_t)end __attribute__((swift_name("init(feature:value:start:end:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSkikoFontFeatureCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t _tag __attribute__((swift_name("_tag")));
@property (readonly) uint32_t end __attribute__((swift_name("end")));
@property (readonly) uint32_t start __attribute__((swift_name("start")));
@property (readonly) NSString *tag __attribute__((swift_name("tag")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoShapingOptions.Companion")))
@interface SharedSkikoShapingOptionsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoShapingOptionsCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedSkikoShapingOptions *DEFAULT __attribute__((swift_name("DEFAULT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontMgr.Companion")))
@interface SharedSkikoFontMgrCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontMgrCompanion *shared __attribute__((swift_name("shared")));
@property (readonly, getter=default) SharedSkikoFontMgr *default_ __attribute__((swift_name("default_")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontStyleSet")))
@interface SharedSkikoFontStyleSet : SharedSkikoRefCnt

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr __attribute__((swift_name("init(ptr:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (instancetype)initWithPtr:(void * _Nullable)ptr allowClose:(BOOL)allowClose __attribute__((swift_name("init(ptr:allowClose:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedSkikoFontStyleSetCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)count __attribute__((swift_name("count()")));
- (SharedSkikoFontStyle *)getStyleIndex:(int32_t)index __attribute__((swift_name("getStyle(index:)")));
- (NSString *)getStyleNameIndex:(int32_t)index __attribute__((swift_name("getStyleName(index:)")));
- (SharedSkikoTypeface * _Nullable)getTypefaceIndex:(int32_t)index __attribute__((swift_name("getTypeface(index:)")));
- (SharedSkikoTypeface * _Nullable)matchStyleStyle:(SharedSkikoFontStyle *)style __attribute__((swift_name("matchStyle(style:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontFeature.Companion")))
@interface SharedSkikoFontFeatureCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontFeatureCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinArray<SharedSkikoFontFeature *> *)parseStr:(NSString *)str __attribute__((swift_name("parse(str:)")));
- (SharedSkikoFontFeature *)parseOneS:(NSString *)s __attribute__((swift_name("parseOne(s:)")));
- (SharedKotlinArray<SharedSkikoFontFeature *> *)parseW3Str:(NSString *)str __attribute__((swift_name("parseW3(str:)")));
@property (readonly) SharedKotlinArray<SharedSkikoFontFeature *> *EMPTY __attribute__((swift_name("EMPTY")));
@property (readonly) uint32_t GLOBAL_END __attribute__((swift_name("GLOBAL_END")));
@property (readonly) uint32_t GLOBAL_START __attribute__((swift_name("GLOBAL_START")));
@property (readonly) SharedSkikoPattern *_featurePattern __attribute__((swift_name("_featurePattern")));
@property (readonly) SharedSkikoPattern *_splitPattern __attribute__((swift_name("_splitPattern")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoFontStyleSet.Companion")))
@interface SharedSkikoFontStyleSetCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSkikoFontStyleSetCompanion *shared __attribute__((swift_name("shared")));
- (SharedSkikoFontStyleSet *)makeEmpty __attribute__((swift_name("makeEmpty()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoPattern")))
@interface SharedSkikoPattern : SharedBase
- (instancetype)initWithRegex:(NSString *)regex __attribute__((swift_name("init(regex:)"))) __attribute__((objc_designated_initializer));
- (SharedSkikoMatcher *)matcherInput:(id)input __attribute__((swift_name("matcher(input:)")));
- (SharedKotlinArray<NSString *> *)splitInput:(id)input __attribute__((swift_name("split(input:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkikoMatcher")))
@interface SharedSkikoMatcher : SharedBase
- (instancetype)initWithRegex:(SharedKotlinRegex *)regex input:(id)input __attribute__((swift_name("init(regex:input:)"))) __attribute__((objc_designated_initializer));
- (NSString * _Nullable)groupIx:(int32_t)ix __attribute__((swift_name("group(ix:)")));
- (BOOL)matches __attribute__((swift_name("matches()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinRegex")))
@interface SharedKotlinRegex : SharedBase
- (instancetype)initWithPattern:(NSString *)pattern __attribute__((swift_name("init(pattern:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPattern:(NSString *)pattern options:(NSSet<SharedKotlinRegexOption *> *)options __attribute__((swift_name("init(pattern:options:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPattern:(NSString *)pattern option:(SharedKotlinRegexOption *)option __attribute__((swift_name("init(pattern:option:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinRegexCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsMatchInInput:(id)input __attribute__((swift_name("containsMatchIn(input:)")));
- (id<SharedKotlinMatchResult> _Nullable)findInput:(id)input startIndex:(int32_t)startIndex __attribute__((swift_name("find(input:startIndex:)")));
- (id<SharedKotlinSequence>)findAllInput:(id)input startIndex:(int32_t)startIndex __attribute__((swift_name("findAll(input:startIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.7")
*/
- (id<SharedKotlinMatchResult> _Nullable)matchAtInput:(id)input index:(int32_t)index __attribute__((swift_name("matchAt(input:index:)")));
- (id<SharedKotlinMatchResult> _Nullable)matchEntireInput:(id)input __attribute__((swift_name("matchEntire(input:)")));
- (BOOL)matchesInput:(id)input __attribute__((swift_name("matches(input:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.7")
*/
- (BOOL)matchesAtInput:(id)input index:(int32_t)index __attribute__((swift_name("matchesAt(input:index:)")));
- (NSString *)replaceInput:(id)input transform:(id (^)(id<SharedKotlinMatchResult>))transform __attribute__((swift_name("replace(input:transform:)")));
- (NSString *)replaceInput:(id)input replacement:(NSString *)replacement __attribute__((swift_name("replace(input:replacement:)")));
- (NSString *)replaceFirstInput:(id)input replacement:(NSString *)replacement __attribute__((swift_name("replaceFirst(input:replacement:)")));
- (NSArray<NSString *> *)splitInput:(id)input limit:(int32_t)limit __attribute__((swift_name("split(input:limit:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.6")
*/
- (id<SharedKotlinSequence>)splitToSequenceInput:(id)input limit:(int32_t)limit __attribute__((swift_name("splitToSequence(input:limit:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSSet<SharedKotlinRegexOption *> *options __attribute__((swift_name("options")));
@property (readonly) NSString *pattern __attribute__((swift_name("pattern")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinRegexOption")))
@interface SharedKotlinRegexOption : SharedKotlinEnum<SharedKotlinRegexOption *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinRegexOption *ignoreCase __attribute__((swift_name("ignoreCase")));
@property (class, readonly) SharedKotlinRegexOption *multiline __attribute__((swift_name("multiline")));
@property (class, readonly) SharedKotlinRegexOption *literal __attribute__((swift_name("literal")));
@property (class, readonly) SharedKotlinRegexOption *unixLines __attribute__((swift_name("unixLines")));
@property (class, readonly) SharedKotlinRegexOption *comments __attribute__((swift_name("comments")));
@property (class, readonly) SharedKotlinRegexOption *dotMatchesAll __attribute__((swift_name("dotMatchesAll")));
@property (class, readonly) SharedKotlinRegexOption *canonEq __attribute__((swift_name("canonEq")));
+ (SharedKotlinArray<SharedKotlinRegexOption *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinRegexOption *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinRegex.Companion")))
@interface SharedKotlinRegexCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinRegexCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)escapeLiteral:(NSString *)literal __attribute__((swift_name("escape(literal:)")));
- (NSString *)escapeReplacementLiteral:(NSString *)literal __attribute__((swift_name("escapeReplacement(literal:)")));
- (SharedKotlinRegex *)fromLiteralLiteral:(NSString *)literal __attribute__((swift_name("fromLiteral(literal:)")));
@end

__attribute__((swift_name("KotlinMatchResult")))
@protocol SharedKotlinMatchResult
@required
- (id<SharedKotlinMatchResult> _Nullable)next __attribute__((swift_name("next()")));
@property (readonly) SharedKotlinMatchResultDestructured *destructured __attribute__((swift_name("destructured")));
@property (readonly) NSArray<NSString *> *groupValues __attribute__((swift_name("groupValues")));
@property (readonly) id<SharedKotlinMatchGroupCollection> groups __attribute__((swift_name("groups")));
@property (readonly) SharedKotlinIntRange *range __attribute__((swift_name("range")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinMatchResultDestructured")))
@interface SharedKotlinMatchResultDestructured : SharedBase
- (NSString *)component1 __attribute__((swift_name("component1()")));
- (NSString *)component10 __attribute__((swift_name("component10()")));
- (NSString *)component2 __attribute__((swift_name("component2()")));
- (NSString *)component3 __attribute__((swift_name("component3()")));
- (NSString *)component4 __attribute__((swift_name("component4()")));
- (NSString *)component5 __attribute__((swift_name("component5()")));
- (NSString *)component6 __attribute__((swift_name("component6()")));
- (NSString *)component7 __attribute__((swift_name("component7()")));
- (NSString *)component8 __attribute__((swift_name("component8()")));
- (NSString *)component9 __attribute__((swift_name("component9()")));
- (NSArray<NSString *> *)toList __attribute__((swift_name("toList()")));
@property (readonly) id<SharedKotlinMatchResult> match __attribute__((swift_name("match")));
@end

__attribute__((swift_name("KotlinMatchGroupCollection")))
@protocol SharedKotlinMatchGroupCollection <SharedKotlinCollection>
@required
- (SharedKotlinMatchGroup * _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
@end

__attribute__((swift_name("KotlinIntProgression")))
@interface SharedKotlinIntProgression : SharedBase <SharedKotlinIterable>
@property (class, readonly, getter=companion) SharedKotlinIntProgressionCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (SharedKotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t first __attribute__((swift_name("first")));
@property (readonly) int32_t last __attribute__((swift_name("last")));
@property (readonly) int32_t step __attribute__((swift_name("step")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntRange")))
@interface SharedKotlinIntRange : SharedKotlinIntProgression <SharedKotlinClosedRange, SharedKotlinOpenEndRange>
- (instancetype)initWithStart:(int32_t)start endInclusive:(int32_t)endInclusive __attribute__((swift_name("init(start:endInclusive:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinIntRangeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)containsValue:(SharedInt *)value __attribute__((swift_name("contains(value:)")));
- (BOOL)containsValue_:(SharedInt *)value __attribute__((swift_name("contains(value_:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEmpty __attribute__((swift_name("isEmpty()")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.9")
*/
@property (readonly) SharedInt *endExclusive __attribute__((swift_name("endExclusive"))) __attribute__((deprecated("Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")));
@property (readonly) SharedInt *endInclusive __attribute__((swift_name("endInclusive")));
@property (readonly, getter=start_) SharedInt *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinMatchGroup")))
@interface SharedKotlinMatchGroup : SharedBase
- (instancetype)initWithValue:(NSString *)value range:(SharedKotlinIntRange *)range __attribute__((swift_name("init(value:range:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinMatchGroup *)doCopyValue:(NSString *)value range:(SharedKotlinIntRange *)range __attribute__((swift_name("doCopy(value:range:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinIntRange *range __attribute__((swift_name("range")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntProgression.Companion")))
@interface SharedKotlinIntProgressionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinIntProgressionCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinIntProgression *)fromClosedRangeRangeStart:(int32_t)rangeStart rangeEnd:(int32_t)rangeEnd step:(int32_t)step __attribute__((swift_name("fromClosedRange(rangeStart:rangeEnd:step:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntRange.Companion")))
@interface SharedKotlinIntRangeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinIntRangeCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinIntRange *EMPTY __attribute__((swift_name("EMPTY")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
