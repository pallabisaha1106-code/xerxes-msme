import { useEffect, useState } from "react";
import { Button, Card, Tag, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as storefrontSiteApi from "../../api/storefrontSiteApi";
import type { StorefrontSettings, TemplateManifest } from "../../api/storefrontSiteApi";

export function StorefrontSitePage() {
  const toast = useToast();
  const [templates, setTemplates] = useState<TemplateManifest[]>([]);
  const [settings, setSettings] = useState<StorefrontSettings | null>(null);
  const [content, setContent] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [previewNonce, setPreviewNonce] = useState(0);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [templateList, currentSettings] = await Promise.all([
          storefrontSiteApi.listTemplates(),
          storefrontSiteApi.getSettings(),
        ]);
        setTemplates(templateList);
        setSettings(currentSettings);
        setContent(currentSettings.content);
      } catch (err) {
        toast.show({ severity: "error", summary: "Failed to load storefront settings", detail: extractErrorMessage(err) });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const template = templates.find((t) => t.id === settings?.templateId);

  function updateField(key: string, value: string) {
    setContent((current) => ({ ...current, [key]: value }));
  }

  async function handleSave() {
    if (!settings) return;
    setSaving(true);
    try {
      const updated = await storefrontSiteApi.updateSettings(settings.templateId, content);
      setSettings(updated);
      setContent(updated.content);
      toast.show({ severity: "success", summary: "Storefront content saved" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  async function handlePublish() {
    setPublishing(true);
    try {
      await handleSave();
      const result = await storefrontSiteApi.publish();
      setSettings((current) => (current ? { ...current, publishedAt: result.publishedAt, previewUrl: result.previewUrl } : current));
      setPreviewNonce((n) => n + 1);
      toast.show({ severity: "success", summary: "Storefront generated", detail: "Preview updated below." });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not generate site", detail: extractErrorMessage(err) });
    } finally {
      setPublishing(false);
    }
  }

  async function handleDownload() {
    setDownloading(true);
    try {
      await storefrontSiteApi.downloadZip();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not download", detail: extractErrorMessage(err) });
    } finally {
      setDownloading(false);
    }
  }

  return (
    <Card
      title="Storefront Site Builder"
      actions={
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button variant="outlined" severity="secondary" loading={saving} onClick={handleSave}>
            Save
          </Button>
          <Button severity="success" loading={publishing} onClick={handlePublish}>
            Generate &amp; Preview
          </Button>
          <Button
            variant="outlined"
            severity="primary"
            icon="download"
            loading={downloading}
            disabled={!settings?.publishedAt}
            onClick={handleDownload}
          >
            Download ZIP
          </Button>
        </div>
      }
    >
      {loading && <p>Loading&hellip;</p>}

      {!loading && settings && (
        <>
          <p style={{ marginBottom: "1rem" }}>
            Template: <strong>{template?.name ?? settings.templateId}</strong>
            {settings.publishedAt ? (
              <Tag severity="success">Published {new Date(settings.publishedAt).toLocaleString()}</Tag>
            ) : (
              <Tag severity="secondary">Not published yet</Tag>
            )}
          </p>
          {template?.description && <p className="field-hint">{template.description}</p>}

          {template?.sections.map((section) => (
            <fieldset key={section.key} style={{ border: "1px solid var(--color-border, #e3d8d2)", borderRadius: 8, padding: "1rem", marginBottom: "1rem" }}>
              <legend style={{ fontWeight: 700, padding: "0 0.5rem" }}>{section.label}</legend>
              {section.fields.map((field) => (
                <div className="field" key={field.key}>
                  <label className="field-label" htmlFor={field.key}>
                    {field.label}
                  </label>
                  {field.type === "textarea" ? (
                    <textarea
                      id={field.key}
                      className="input"
                      rows={3}
                      value={content[field.key] ?? ""}
                      onChange={(e) => updateField(field.key, e.target.value)}
                    />
                  ) : (
                    <input
                      id={field.key}
                      className="input"
                      type="text"
                      value={content[field.key] ?? ""}
                      onChange={(e) => updateField(field.key, e.target.value)}
                    />
                  )}
                </div>
              ))}
            </fieldset>
          ))}

          {settings.previewUrl && (
            <div>
              <h3>Preview</h3>
              <iframe
                title="Storefront preview"
                src={`${settings.previewUrl}?v=${previewNonce}`}
                style={{ width: "100%", height: 600, border: "1px solid var(--color-border, #e3d8d2)", borderRadius: 8 }}
              />
            </div>
          )}
        </>
      )}
    </Card>
  );
}
