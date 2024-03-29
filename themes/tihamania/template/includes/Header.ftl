<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<div class="container-fluid">
  <div class="row align-items-center">
    <div class="col">
      <a class="navbar-brand" href="<@ofbizUrl>main</@ofbizUrl>">
          <#if sessionAttributes.overrideLogo??>
            <img src="<@ofbizContentUrl>${sessionAttributes.overrideLogo}</@ofbizContentUrl>" alt="Logo"/>
          <#elseif catalogHeaderLogo??>
            <img src="<@ofbizContentUrl>${catalogHeaderLogo}</@ofbizContentUrl>" alt="Logo"/>
          <#elseif layoutSettings.VT_HDR_IMAGE_URL?has_content>
            <img src="<@ofbizContentUrl>${layoutSettings.VT_HDR_IMAGE_URL}</@ofbizContentUrl>" alt="Logo"/>
          </#if>
        </a>
    </div>
    <div class="col text-center d-none d-lg-block">
      <#if !productStore??>
            <h3>${uiLabelMap.EcommerceNoProductStore}</h3>
          </#if>
          <#if (productStore.title)??>
            <h3>${productStore.title}</h3>
           </#if>
          <#if (productStore.subtitle)??>
            <div id="company-subtitle">${productStore.subtitle}</div>
          </#if>
          <div>
            <#if sessionAttributes.autoName?has_content>
              <span class="text-success">${uiLabelMap.CommonWelcome}&nbsp;${sessionAttributes.autoName}!</span>
              (${uiLabelMap.CommonNotYou}?&nbsp;
              <a href="<@ofbizUrl>autoLogout</@ofbizUrl>" class="linktext">${uiLabelMap.CommonClickHere}</a>)
            <#else>
              ${uiLabelMap.CommonWelcome}!
            </#if>
          </div>
    </div>
    <div class="col">
      ${screens.render("component://ecommerce/widget/CartScreens.xml#microcart")}
    </div>
  </div>
</div>

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
    <span class="navbar-toggler-icon"></span>
  </button>
  <div class="collapse navbar-collapse" id="navbarContent">
    <ul class="navbar-nav ml-auto">
      <#list completedTree[0].child?sort_by("productCategoryId") as root>
                <li class="nav-item">
                  <a class="nav-link" href="<@ofbizCatalogAltUrl productCategoryId=root.productCategoryId/>">${root.categoryName}</a>
                </li>

            </#list>
    </ul>
    <ul class="navbar-nav mr-auto">
      <#if userLogin?has_content && userLogin.userLoginId != "anonymous">
        <li class="nav-item">
          <a class="nav-link" href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a>
        </li>
      <#else>
        <li class="nav-item">
          <a class="nav-link" href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="<@ofbizUrl>newcustomer</@ofbizUrl>">${uiLabelMap.EcommerceRegister}</a>
        </li>
      </#if>
      <li class="nav-item">
        <#if userLogin?has_content && userLogin.userLoginId != "anonymous">
          <a class="nav-link" href="<@ofbizUrl>contactus</@ofbizUrl>">${uiLabelMap.CommonContactUs}</a>
        <#else>
          <a class="nav-link" href="<@ofbizUrl>AnonContactus</@ofbizUrl>">${uiLabelMap.CommonContactUs}</a>
        </#if>
      </li>
      <li class="nav-item">
        <a class="nav-link" href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonMain}</a>
      </li>
    </ul>
  </div>
</nav>

<script type="application/javascript">

function callDocument(id, parentCategoryStr) {
    var checkUrl = '<@ofbizUrl>productCategoryList</@ofbizUrl>';
    if (checkUrl.search("http"))
      var ajaxUrl = '<@ofbizUrl>productCategoryList</@ofbizUrl>';
    else
      var ajaxUrl = '<@ofbizUrl>productCategoryListSecure</@ofbizUrl>';

    //jQuerry Ajax Request
    jQuery.ajax({
      url: ajaxUrl,
      type: 'POST',
      data: {"category_id": id, "parentCategoryStr": parentCategoryStr},
      error: function (msg) {
        alert("An error occurred loading content! : " + msg);
      },
      success: function (msg) {
        jQuery('#div3').html(msg);
      }
    });
  }

  </script>




