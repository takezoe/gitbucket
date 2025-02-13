package gitbucket.core.controller

import gitbucket.core.api.*
import gitbucket.core.controller.api.*
import gitbucket.core.service.*
import gitbucket.core.util.Implicits.*
import gitbucket.core.util.*
import gitbucket.core.plugin.PluginRegistry

class ApiController
    extends ApiControllerBase
    with ApiGitReferenceControllerBase
    with ApiIssueCommentControllerBase
    with ApiIssueControllerBase
    with ApiIssueLabelControllerBase
    with ApiIssueMilestoneControllerBase
    with ApiOrganizationControllerBase
    with ApiPullRequestControllerBase
    with ApiReleaseControllerBase
    with ApiRepositoryBranchControllerBase
    with ApiRepositoryCollaboratorControllerBase
    with ApiRepositoryCommitControllerBase
    with ApiRepositoryContentsControllerBase
    with ApiRepositoryControllerBase
    with ApiRepositoryStatusControllerBase
    with ApiRepositoryWebhookControllerBase
    with ApiUserControllerBase
    with RepositoryService
    with AccountService
    with ProtectedBranchService
    with IssuesService
    with LabelsService
    with MilestonesService
    with PullRequestService
    with CommitsService
    with CommitStatusService
    with ReleaseService
    with RepositoryCreationService
    with RepositoryCommitFileService
    with IssueCreationService
    with HandleCommentService
    with MergeService
    with WebHookService
    with WebHookPullRequestService
    with WebHookIssueCommentService
    with WebHookPullRequestReviewCommentService
    with WikiService
    with ActivityService
    with PrioritiesService
    with AdminAuthenticator
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator
    with RequestCache

trait ApiControllerBase extends ControllerBase {

  /**
   * 404 for non-implemented api
   */
  get("/api/v3/*") {
    NotFound()
  }
  post("/api/v3/*") {
    NotFound()
  }
  put("/api/v3/*") {
    NotFound()
  }
  delete("/api/v3/*") {
    NotFound()
  }
  patch("/api/v3/*") {
    NotFound()
  }

  /**
   * https://developer.github.com/v3/#root-endpoint
   */
  get("/api/v3") {
    JsonFormat(ApiEndPoint())
  }

  /**
   * @see https://developer.github.com/v3/rate_limit/#get-your-current-rate-limit-status
   * but not enabled.
   */
  get("/api/v3/rate_limit") {
    contentType = formats("json")
    // this message is same as github enterprise...
    org.scalatra.NotFound(ApiError("Rate limiting is not enabled."))
  }

  /**
   * non-GitHub compatible API for listing plugins
   */
  get("/api/v3/gitbucket/plugins") {
    PluginRegistry().getPlugins().map { ApiPlugin(_) }
  }

  /**
   * https://docs.github.com/en/enterprise-server@2.21/rest/reference/meta#get-github-enterprise-server-meta-information
   */
  get("/api/v3/meta") {
    JsonFormat(
      Map(
        "https://api.github.com/meta" -> context.loginAccount.isDefined,
        "installed_version" -> "2.21.0"
      )
    )
  }
}
