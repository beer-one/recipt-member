package com.recipt.member.presentation.router

import com.recipt.member.presentation.handler.MemberHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class MemberRouter (
    private val memberHandler: MemberHandler
) {

    @Bean
    fun memberRoute(): RouterFunction<ServerResponse> {
        return coRouter {
            "/members".nest {
                accept(MediaType.APPLICATION_JSON).nest {
                    POST("", memberHandler::signUp)

                    "/profile".nest {
                        GET("/{memberNo}", memberHandler::getProfile)

                        "/me".nest {
                            GET("", memberHandler::getMyProfile)
                            PUT("", memberHandler::modifyMyProfile)
                        }
                    }

                    "/following".nest {
                        GET("", memberHandler::getFollowingProfileList)
                        GET("/check/{memberNo}", memberHandler::checkFollowing)
                        POST("", memberHandler::follow)
                        DELETE("", memberHandler::unfollow)
                    }
                }
            }
        }
    }
}