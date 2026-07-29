import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Modal, Card, PageHeader, StatCard, Tag, EmptyState } from '../components/ui';
import {
  listProducts,
  listDocuments,
  uploadDocument,
  getDocumentContent,
  previewDocumentUrl,
  submitRevision,
  publishRevision,
  approveRevision,
  deprecateRevision,
  archiveRevision,
  restoreRevision,
  deleteDocument,
} from '../lib/api';
import type { Product, KnowledgeDocument } from '../lib/types';

const docTone: Record<string, 'ok' | 'warn' | 'danger' | 'info' | 'mute'> = {
  UPLOADED: 'warn',
  SUBMITTED: 'warn',
  APPROVED: 'info',
  PUBLISHED: 'ok',
  DEPRECATED: 'danger',
  ARCHIVED: 'mute',
};

export default function DocumentsPage() {
  const { t, i18n } = useTranslation();
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [productFilter, setProductFilter] = useState('');
  const [languageFilter, setLanguageFilter] = useState('');
  const [showUpload, setShowUpload] = useState(false);
  const [uploadForm, setUploadForm] = useState({ title: '', language: 'zh-CN', region: 'EU', productModelId: '' });
  const [file, setFile] = useState<File | null>(null);
  const [viewing, setViewing] = useState<{ doc: KnowledgeDocument; content: string } | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const polling = useRef(false);
  const [error, setError] = useState<string | null>(null);
  const [manageTarget, setManageTarget] = useState<KnowledgeDocument | null>(null);
  const [actionMsg, setActionMsg] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<KnowledgeDocument | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [d, p] = await Promise.all([listDocuments(), listProducts()]);
      setDocuments(d);
      setProducts(p);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [d, p] = await Promise.all([listDocuments(), listProducts()]);
        if (!active) return;
        setDocuments(d);
        setProducts(p);
      } catch (e) {
        if (!active) return;
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const timer = setInterval(async () => {
      if (polling.current) return;
      polling.current = true;
      try {
        const d = await listDocuments();
        setDocuments((prev) => {
          const processing = prev.some((x) => x.status === 'uploaded' || x.status === 'parsing');
          return processing ? d : prev;
        });
      } finally {
        polling.current = false;
      }
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  const filtered = documents.filter(
    (d) =>
      (!statusFilter || d.status === statusFilter) &&
      (!productFilter || d.scope?.productModelId === productFilter) &&
      (!languageFilter || d.locale === languageFilter),
  );

  const doUpload = async () => {
    if (!file) return;
    await uploadDocument({
      file,
      title: uploadForm.title,
      locale: uploadForm.language,
      region: uploadForm.region,
      productModelId: uploadForm.productModelId || undefined,
    });
    setShowUpload(false);
    setFile(null);
    setUploadForm({ title: '', language: i18n.language, region: 'EU', productModelId: '' });
    void load();
  };

  const viewContent = async (doc: KnowledgeDocument) => {
    const content = await getDocumentContent(doc.id);
    setViewing({ doc, content });
  };

  const openManage = (d: KnowledgeDocument) => {
    setManageTarget(d);
    setActionMsg(null);
  };

  const runRevisionAction = async (action: 'submit' | 'publish' | 'approve' | 'deprecate' | 'archive' | 'restore') => {
    if (!manageTarget?.revisionId) {
      setActionMsg(t('documents.noRevision'));
      return;
    }
    setActionMsg(null);
    try {
      if (action === 'submit') await submitRevision(manageTarget.revisionId);
      else if (action === 'publish') await publishRevision(manageTarget.revisionId);
      else if (action === 'approve') await approveRevision(manageTarget.revisionId);
      else if (action === 'deprecate') await deprecateRevision(manageTarget.revisionId);
      else if (action === 'archive') await archiveRevision(manageTarget.revisionId);
      else if (action === 'restore') await restoreRevision(manageTarget.revisionId);
      setActionMsg(t('common.saved'));
      await load();
      const updated = documents.find((x) => x.id === manageTarget.id);
      if (updated) setManageTarget(updated);
    } catch (e) {
      setActionMsg(t('documents.actionFailed', { msg: e instanceof Error ? e.message : String(e) }));
    }
  };

  const openDelete = (d: KnowledgeDocument) => {
    setDeleteTarget(d);
    setDeleteOpen(true);
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteDocument(deleteTarget.id);
      setDeleteOpen(false);
      setDeleteTarget(null);
      await load();
    } catch (e) {
      setActionMsg(t('documents.actionFailed', { msg: e instanceof Error ? e.message : String(e) }));
    }
  };

  if (loading) return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;
  if (error)
    return (
      <div className="enter p-6">
        <Card className="flex flex-col items-center gap-3 py-16 text-ink2">
          <span className="text-sm text-ink">{error}</span>
          <Button variant="default" size="sm" onClick={() => load()}>
            {t('common.retry')}
          </Button>
        </Card>
      </div>
    );

  const ready = documents.filter((d) => d.status === 'PUBLISHED').length;
  const processing = documents.filter((d) => d.status === 'UPLOADED' || d.status === 'SUBMITTED' || d.status === 'APPROVED').length;
  const archived = documents.filter((d) => d.status === 'DEPRECATED' || d.status === 'ARCHIVED').length;

  const statusOptions = ['UPLOADED', 'SUBMITTED', 'APPROVED', 'PUBLISHED', 'DEPRECATED', 'ARCHIVED'];

  return (
    <div className="enter">
      <PageHeader
        title={t('documents.title')}
        subtitle={t('documents.subtitle')}
        icon="❏"
        actions={
          <Button variant="primary" onClick={() => setShowUpload(true)}>
            {t('documents.new')}
          </Button>
        }
      />

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label={t('documents.title')} value={documents.length} tone="brand" />
        <StatCard label={t('status.published')} value={ready} tone="ok" />
        <StatCard label={t('common.processing')} value={processing} tone="ai" />
        <StatCard label={t('status.archived')} value={archived} tone="safety" />
      </div>

      <Card className="mb-4 flex flex-wrap items-center gap-3 p-3">
        <select
          className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">{t('common.status')}: {t('common.all')}</option>
          {statusOptions.map((s) => (
            <option key={s} value={s}>
              {t(`status.${s.toLowerCase()}`)}
            </option>
          ))}
        </select>
        <select
          className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink"
          value={productFilter}
          onChange={(e) => setProductFilter(e.target.value)}
        >
          <option value="">{t('documents.productPlaceholder')}</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.displayName}
            </option>
          ))}
        </select>
        <select
          className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink"
          value={languageFilter}
          onChange={(e) => setLanguageFilter(e.target.value)}
        >
          <option value="">{t('common.language')}: {t('common.all')}</option>
          <option value="zh-CN">简体中文</option>
          <option value="en">English</option>
          <option value="de-DE">Deutsch</option>
          <option value="fr-FR">Français</option>
          <option value="es-ES">Español</option>
        </select>
      </Card>

      {filtered.length === 0 ? (
        <Card>
          <EmptyState title={t('documents.emptyTitle')} hint={t('documents.emptyHint')} action={<Button variant="primary" onClick={() => setShowUpload(true)}>{t('documents.new')}</Button>} />
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-brand-soft/50 text-ink2">
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thTitle')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thLanguage')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thRegion')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thProduct')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thStatus')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('documents.thActions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((d) => (
                <tr key={d.id} className="border-t border-line transition hover:bg-brand-soft/40">
                  <td className="px-4 py-3 font-medium text-ink">{d.title}</td>
                  <td className="px-4 py-3 text-ink2">{d.locale}</td>
                  <td className="px-4 py-3 text-ink2">{d.region}</td>
                  <td className="px-4 py-3 text-ink2">{d.productName ?? '—'}</td>
                  <td className="px-4 py-3">
                    <Tag tone={docTone[d.status] ?? 'mute'}>{t(`status.${d.status.toLowerCase()}`)}</Tag>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <Button variant="ghost" size="sm" onClick={() => viewContent(d)}>
                        {t('documents.preview')}
                      </Button>
                      <Button variant="default" size="sm" onClick={() => openManage(d)}>
                        {t('documents.manage')}
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => openDelete(d)}>
                        {t('common.delete')}
                      </Button>
                      <a
                        className="inline-flex h-8 items-center rounded-lg border border-line px-2.5 text-xs font-semibold text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
                        href={previewDocumentUrl(d.id)}
                        target="_blank"
                        rel="noreferrer"
                      >
                        ↗
                      </a>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      <Modal
        open={showUpload}
        title={t('documents.uploadTitle')}
        onClose={() => setShowUpload(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowUpload(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="primary" onClick={doUpload} disabled={!file}>
              {t('documents.upload')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('documents.titleField')}</label>
            <Input value={uploadForm.title} onChange={(e) => setUploadForm({ ...uploadForm, title: e.target.value })} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('documents.language')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={uploadForm.language}
                onChange={(e) => setUploadForm({ ...uploadForm, language: e.target.value })}
              >
                <option value="zh-CN">简体中文</option>
                <option value="en">English</option>
                <option value="de-DE">Deutsch</option>
                <option value="fr-FR">Français</option>
                <option value="es-ES">Español</option>
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('documents.region')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={uploadForm.region}
                onChange={(e) => setUploadForm({ ...uploadForm, region: e.target.value })}
              >
                {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('documents.productModelId')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={uploadForm.productModelId}
              onChange={(e) => setUploadForm({ ...uploadForm, productModelId: e.target.value })}
            >
              <option value="">{t('documents.productPlaceholder')}</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('documents.file')}</label>
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.docx,.png,.jpeg,.jpg"
              className="block w-full text-sm text-ink2 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-soft file:px-3 file:py-1.5 file:text-brand-700 file:font-semibold"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
        </div>
      </Modal>

      <Modal
        open={!!viewing}
        title={viewing ? viewing.doc.title : ''}
        onClose={() => setViewing(null)}
        widthClass="max-w-2xl"
      >
        {viewing && (
          <div className="space-y-3 text-sm">
            <div className="flex flex-wrap gap-2 text-xs text-ink2">
              <Tag tone="mute">{viewing.doc.language}</Tag>
              <Tag tone="mute">{viewing.doc.region}</Tag>
              {viewing.doc.scope?.productModelId && <Tag tone="mute">{viewing.doc.scope.productModelId}</Tag>}
            </div>
            {viewing.doc.chunkCount != null && (
              <div className="text-ink2">{t('documents.chunks', { count: viewing.doc.chunkCount })}</div>
            )}
            <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-xl bg-slate-50 p-3 font-sans text-ink">
              {viewing.content || t('documents.noContent')}
            </pre>
          </div>
        )}
      </Modal>

      <Modal
        open={!!manageTarget}
        title={manageTarget ? manageTarget.title : ''}
        onClose={() => setManageTarget(null)}
        widthClass="max-w-lg"
      >
        {manageTarget && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2 text-xs text-ink2">
              <Tag tone="mute">{manageTarget.locale}</Tag>
              <Tag tone="mute">{manageTarget.region}</Tag>
              <Tag tone={docTone[manageTarget.status] ?? 'mute'}>{t(`status.${manageTarget.status.toLowerCase()}`)}</Tag>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-semibold text-ink">{t('documents.manage')}</p>
              <div className="flex flex-wrap gap-2">
                {manageTarget.status === 'UPLOADED' && (
                  <Button size="sm" variant="primary" onClick={() => runRevisionAction('submit')}>
                    {t('flows.submit')}
                  </Button>
                )}
                {manageTarget.status === 'SUBMITTED' && (
                  <>
                    <Button size="sm" variant="primary" onClick={() => runRevisionAction('approve')}>
                      {t('flows.approve')}
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => runRevisionAction('deprecate')}>
                      {t('flows.deprecate')}
                    </Button>
                  </>
                )}
                {manageTarget.status === 'APPROVED' && (
                  <>
                    <Button size="sm" variant="primary" onClick={() => runRevisionAction('publish')}>
                      {t('flows.publish')}
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => runRevisionAction('deprecate')}>
                      {t('flows.deprecate')}
                    </Button>
                  </>
                )}
                {manageTarget.status === 'PUBLISHED' && (
                  <>
                    <Button size="sm" variant="danger" onClick={() => runRevisionAction('deprecate')}>
                      {t('flows.deprecate')}
                    </Button>
                    <Button size="sm" variant="default" onClick={() => runRevisionAction('archive')}>
                      {t('products.archive')}
                    </Button>
                  </>
                )}
                {(manageTarget.status === 'DEPRECATED' || manageTarget.status === 'ARCHIVED') && (
                  <Button size="sm" variant="default" onClick={() => runRevisionAction('restore')}>
                    {t('flows.restore')}
                  </Button>
                )}
              </div>
            </div>
            {actionMsg && <p className="text-sm text-ink2">{actionMsg}</p>}
          </div>
        )}
      </Modal>

      <Modal
        open={deleteOpen}
        title={t('common.delete')}
        onClose={() => setDeleteOpen(false)}
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setDeleteOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="danger" size="sm" onClick={confirmDelete}>
              {t('common.confirm')}
            </Button>
          </>
        }
      >
        <p className="text-sm text-ink2">{t('documents.deleteConfirm')}</p>
        <p className="mt-2 text-xs text-ink2/70">{t('documents.deleteHint')}</p>
      </Modal>
    </div>
  );
}
