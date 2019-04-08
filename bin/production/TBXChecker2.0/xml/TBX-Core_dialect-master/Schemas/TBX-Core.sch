<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2"
    xmlns:sqf="http://www.schematron-quickfix.com/validator/process">
    <ns uri="urn:iso:std:iso:30042:ed-2" prefix="tbx" />
    
    <pattern id="coreEnforecement">
        <rule context="tbx:termNote">
            <assert test="parent::tbx:termSec or parent::tbx:termNoteGrp/parent::tbx:termSec">Any termNote is only allowed at the termSec level.</assert>
        </rule>
        <rule context="tbx:*[@type]">
            <assert test="@type != ''">Data category must be declared.  If no permitted data categories are listed in the grammar schema, blank values are also not allowed.</assert>
        </rule>
        <rule context="*[@target]">
            <assert test="matches(@target,'https?://.+') or @target = //*/@id">ID must be IDREF for internal references or URI following HTTP protocol for external references.</assert>
        </rule>
    </pattern>
    
    <pattern id="XLIFF.inlineConstraints">
        <rule context="tbx:sc[following-sibling::tbx:ec]">
            <assert test="@isolated='no' or not(@isolated)">@isolated must be 'no' if &lt;sc/&gt; or &lt;ec/&gt; has its corresponding &lt;sc/&gt;/&lt;ec/&gt; in the same note text and @startRef must be used for &lt;ec&gt;</assert>
        </rule>
        <rule context="tbx:ec[preceding-sibling::tbx:sc]">
            <assert test="@isolated='no' or not(@isolated)">@isolated must be 'no' if &lt;sc/&gt; or &lt;ec/&gt; has its corresponding &lt;sc/&gt;/&lt;ec/&gt; in the same note text and @startRef must be used for &lt;ec&gt;</assert>
            <assert test="@startRef">@starRef is required for &lt;ec&gt; if it is in the same note text as its corresponding &lt;sc&gt;</assert>
            <!--<assert test="not(@dir)">@dir only permitted when @isolated is 'yes'.</assert>--><!--@dir IS NOT CURRENTLY USED IN TBX-->
        </rule>
        <rule context="tbx:sc[not(following-sibling::tbx:ec)]">
            <assert test="@isolated='yes' or not(@isolated)">@isolated must be 'yes' if &lt;sc/&gt; or &lt;ec/&gt; does not have its corresponding &lt;sc/&gt;/&lt;sc/&gt; in the same note text</assert>
        </rule>
        <rule context="tbx:ec[not(preceding-sibling::tbx:sc)]">
            <assert test="@isolated='yes' or not(@isolated)">@isolated must be 'yes' if &lt;sc/&gt; or &lt;ec/&gt; does not have its corresponding &lt;sc/&gt;/&lt;sc/&gt; in the same note text</assert>
            <assert test="@id">@id is REQUIRED when @isolated is or should be 'yes'.</assert>
        </rule>
        <rule context="tbx:ec[@isolated='yes']">
            <assert test="@id != ''">ID is required if @isolated is 'yes'.</assert>
        </rule>
    </pattern>
    
    <pattern id="dialectEnforcement">
        <rule context="tbx:tbx">
            <assert test="attribute::type='TBX-Core'">The name of this dialect should be TBX-Core</assert>
        </rule>
        <rule context="*[not(namespace-uri() = 'urn:iso:std:iso:30042:ed-2')]">
            <assert test="false()">No elements from foreign namespaces are permitted in the TBX-Core dialect.</assert>
        </rule>
    </pattern>
</schema>