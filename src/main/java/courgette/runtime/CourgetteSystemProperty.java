package courgette.runtime;

final class CourgetteSystemProperty {
    final static String THREADS = "courgette.threads";
    final static String RUN_LEVEL = "courgette.runLevel";
    final static String RERUN_FAILED_SCENARIOS = "courgette.rerunFailedScenarios";
    final static String RERUN_ATTEMPTS = "courgette.rerunAttempts";
    final static String PERSIST_PARALLEL_CUCUMBER_JSON_REPORTS = "courgette.persistParallelCucumberJsonReports";
    final static String VM_OPTIONS = "courgette.vmoptions";
    final static String REPORT_TITLE = "courgette.reportTitle";
    final static String REPORT_TARGET_DIR = "courgette.reportTargetDir";
    final static String ENVIRONMENT_INFO = "courgette.environmentInfo";
    final static String SLACK_WEBHOOK_URL = "courgette.slackWebhookUrl";
    final static String SLACK_CHANNEL = "courgette.slackChannel";
    final static String SLACK_TEST_ID = "courgette.slackTestId";
    final static String DEVICE_NAME_SYSTEM_PROPERTY = "courgette.mobile.device.name";
    final static String UDID_SYSTEM_PROPERTY = "courgette.mobile.device.udid";
    final static String PARALLEL_PORT_SYSTEM_PROPERTY = "courgette.mobile.device.parallel.port";
    final static String MOBILE_DEVICE_TYPE = "courgette.mobileDeviceType";
    final static String MOBILE_DEVICE = "courgette.mobileDevice";
    final static String REAL_MOBILE_DEVICE_TAG = "courgette.realMobileDeviceTag";
    final static String CLASS_PATH = "courgette.classPath";
    final static String PLUGIN = "courgette.plugin";
    final static String EXCLUDE_FEATURE_FROM_RERUN = "courgette.excludeFeatureFromRerun";
    final static String EXCLUDE_TAG_FROM_RERUN = "courgette.excludeTagFromRerun";
    final static String THREAD_ID = "courgette.threadId";
    final static String THREAD_NAME = "courgette.threadName";
    final static String FIXED_THREAD_DELAY = "courgette.fixedThreadDelay";
    final static String RANDOM_THREAD_DELAY = "courgette.randomThreadDelay";
    final static String GENERATE_COURGETTE_RUN_LOG = "courgette.generateCourgetteRunLog";
}