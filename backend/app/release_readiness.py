"""Editorial readiness declarations required before activation, not legal review."""


def content_readiness_gate(package):
    """Fail closed; a signed pending candidate is still allowed into review.

    Only literal JSON true is accepted. This gate supplements signature,
    structure, impact review and regression checks; it does not replace them.
    """
    if not isinstance(package, dict):
        return False, {"errors": ["invalid_package"], "pending_item_count": 0}
    errors = []
    if package.get("ready") is not True:
        errors.append("package_not_ready")
    items = package.get("items")
    pending = []
    if not isinstance(items, list) or not items:
        errors.append("missing_or_empty_items")
    else:
        for index, item in enumerate(items):
            if not isinstance(item, dict) or item.get("ready") is not True:
                pending.append({
                    "index": index,
                    "stable_key": item.get("stable_key") if isinstance(item, dict) else None,
                })
        if pending:
            errors.append("items_not_ready")
    return not errors, {
        "errors": errors,
        "item_count": len(items) if isinstance(items, list) else 0,
        "pending_item_count": len(pending),
        "pending_items_sample": pending[:20],
    }
