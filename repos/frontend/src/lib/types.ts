/** 与后端契约对应的前端类型定义。 */

export type Product = {
  id: string;
  family: string;
  model: string;
  displayName: string;
  region: string;
  status: string;
  hardwareVersion?: string;
  firmwareMin?: string;
  firmwareMax?: string;
};

export type QrBinding = {
  id: string;
  productModelId: string;
  batch?: string;
  serialNumber?: string;
  status: string;
  expiresAt?: string;
};

export type KnowledgeDoc = {
  id: string;
  revisionId: string | null;
  title: string;
  locale: string;
  status: string;
};

export type ChunkView = { chunkNo: number; source: string; text: string };

export type Preview = { title: string; status: string; text: string; chunks: ChunkView[] };

export type HandoffView = { id: string };

export type FlowControl = {
  flowId: string;
  nodeKey: string;
  nodeType: string;
  expectedInput: string;
  risk: string;
  path: string[];
  totalSteps: number;
  end: boolean;
  escalated: boolean;
};

export type Answer = {
  intent: string;
  content: string;
  citationChunkIds: string[];
  expectedInput?: string;
  risk?: string;
  flowControl?: FlowControl;
};

export type NodeView = {
  id: string;
  nodeKey: string;
  nodeType: string;
  prompt: string;
  risk: string;
  expectedInput: string;
  branchYes?: string;
  branchNo?: string;
  branchUnknown?: string;
  branchNext?: string;
  safetyStop: boolean;
  sourceRefs: string[];
  orderIndex: number;
};

export type FlowView = {
  id: string;
  definitionId: string;
  versionNo: number;
  title: string;
  triggerIntent: string;
  productModelId: string;
  productVariantId?: string;
  hardwareRevision?: string;
  region: string;
  locale: string;
  firmwareMin?: string;
  firmwareMax?: string;
  triggerPhrase?: string;
  priority: number;
  status: string;
  owner?: string;
};

export type FlowDetail = { flow: FlowView; nodes: NodeView[] };

export type SimStep = {
  nodeKey: string;
  nodeType: string;
  prompt: string;
  expectedInput: string;
  risk: string;
  escalated: boolean;
};

export type SimulateResponse = {
  transcript: SimStep[];
  escalated: boolean;
  coverage: { nodes: number; visited: number; unreachable: string[] };
};

export type SearchHit = {
  chunkId: string;
  source: string;
  text: string;
  revisionId: string;
  chunkNo: number | string;
  score: number;
  vector: boolean;
  keyword: boolean;
};

export type SearchResponse = { results: SearchHit[] };
