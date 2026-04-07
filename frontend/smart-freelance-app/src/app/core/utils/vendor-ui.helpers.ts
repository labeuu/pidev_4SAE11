import { VendorApprovalStatus } from '../services/vendor.service';

export function formatVendorShortDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso).slice(0, 10);
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
}

const STATUS_CLASS: Record<VendorApprovalStatus, string> = {
  PENDING: 'badge--warning',
  APPROVED: 'badge--success',
  REJECTED: 'badge--error',
  SUSPENDED: 'badge--error',
  EXPIRED: 'badge--neutral',
};

const STATUS_LABEL: Record<VendorApprovalStatus, string> = {
  PENDING: 'En attente',
  APPROVED: 'Approuvé',
  REJECTED: 'Rejeté',
  SUSPENDED: 'Suspendu',
  EXPIRED: 'Expiré',
};

export function vendorApprovalStatusClass(status: VendorApprovalStatus): string {
  return STATUS_CLASS[status] || '';
}

export function vendorApprovalStatusLabel(status: VendorApprovalStatus): string {
  return STATUS_LABEL[status] || status;
}
